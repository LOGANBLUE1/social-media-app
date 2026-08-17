package com.example.app.services;

import com.example.app.dataAccess.ConversationRepository;
import com.example.app.dataAccess.MessageRepository;
import com.example.app.entities.Conversation;
import com.example.app.entities.ConversationType;
import com.example.app.entities.Message;
import com.example.app.entities.User;
import com.example.app.exceptions.ForbiddenException;
import com.example.app.exceptions.NotFoundException;
import com.example.app.responses.ConversationResponse;
import com.example.app.responses.MessageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    /** Enough for a name to be descriptive without becoming the message body. Matches V6. */
    private static final int MAX_GROUP_NAME = 120;
    /** A cap exists mainly so a single request cannot create an unbounded number of rows. */
    private static final int MAX_GROUP_MEMBERS = 50;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final FriendRequestService friendRequestService;

    public ChatService(ConversationRepository conversationRepository,
                       MessageRepository messageRepository,
                       UserService userService,
                       FriendRequestService friendRequestService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.friendRequestService = friendRequestService;
    }

    /**
     * Opens the direct chat with another user, creating it on first use. Idempotent: asking twice
     * returns the same conversation, because directKey normalises the pair.
     *
     * @throws IllegalArgumentException if userId is missing or is the caller's own id
     * @throws NotFoundException        if userId has no row
     * @throws ForbiddenException       if the two are not friends
     */
    @Transactional
    public ConversationResponse getOrCreate(Long viewerId, Long otherUserId) {
        if (otherUserId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (viewerId.equals(otherUserId)) {
            throw new IllegalArgumentException("You cannot start a conversation with yourself");
        }
        User other = userService.getUserByIdOrThrow(otherUserId);
        requireFriends(viewerId, otherUserId);

        Conversation conversation = conversationRepository
                .findByDirectKey(directKeyFor(viewerId, otherUserId))
                .orElseGet(() -> createDirect(userService.getUserByIdOrThrow(viewerId), other));

        return new ConversationResponse(conversation, viewerId);
    }

    /**
     * Creates a named group and puts the caller in it along with the members they chose.
     *
     * Membership is gated on friendship with the *creator* only, not between every pair -- that is
     * what "add people from your friends list" means, and requiring mutual friendship across the
     * whole group would make most groups impossible to create.
     *
     * @throws IllegalArgumentException if the name is blank, too long, or the member list is empty
     *                                  or oversized
     * @throws NotFoundException        if any member id has no row
     * @throws ForbiddenException       if any member is not a friend of the caller
     */
    @Transactional
    public ConversationResponse createGroup(Long creatorId, String name, List<Long> memberIds) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        if (trimmed.length() > MAX_GROUP_NAME) {
            throw new IllegalArgumentException("name must be " + MAX_GROUP_NAME + " characters or fewer");
        }
        if (memberIds == null || memberIds.isEmpty()) {
            throw new IllegalArgumentException("Pick at least one friend to add");
        }

        // LinkedHashSet: dedupe a list that arrived with repeats, without reordering it. The
        // creator is dropped from the members because they are added separately below -- listing
        // themselves should not be an error.
        Set<Long> uniqueMemberIds = memberIds.stream()
                .filter(id -> id != null && !id.equals(creatorId))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniqueMemberIds.isEmpty()) {
            throw new IllegalArgumentException("Pick at least one friend to add");
        }
        if (uniqueMemberIds.size() + 1 > MAX_GROUP_MEMBERS) {
            throw new IllegalArgumentException("A group can hold at most " + MAX_GROUP_MEMBERS + " people");
        }

        User creator = userService.getUserByIdOrThrow(creatorId);
        LocalDateTime now = LocalDateTime.now();

        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.GROUP);
        conversation.setName(trimmed);
        conversation.setOwner(creator);
        conversation.setLastSeq(0L);
        conversation.setCreateDate(now);
        // Seeded rather than left null so the chat list's ORDER BY never has to deal with nulls.
        conversation.setLastMessageAt(now);
        conversation.addParticipant(creator, now);

        for (Long memberId : uniqueMemberIds) {
            User member = userService.getUserByIdOrThrow(memberId);
            if (!friendRequestService.areFriends(creatorId, memberId)) {
                throw new ForbiddenException("You can only add friends to a group");
            }
            conversation.addParticipant(member, now);
        }

        return new ConversationResponse(conversationRepository.save(conversation), creatorId);
    }

    /** The caller's chats -- direct and group alike -- most recently active first. */
    public List<ConversationResponse> listConversations(Long viewerId) {
        return conversationRepository.findByParticipant(viewerId).stream()
                .map(conversation -> new ConversationResponse(conversation, viewerId))
                .collect(Collectors.toList());
    }

    /**
     * The entire conversation, oldest message first.
     *
     * @throws ForbiddenException if the caller is not a participant
     */
    public List<MessageResponse> listMessages(Long conversationId, Long viewerId) {
        requireParticipant(getWithParticipantsOrThrow(conversationId), viewerId);
        return messageRepository.findByConversationIdOrderBySeqAsc(conversationId).stream()
                .map(MessageResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Appends a message. The conversation row is locked for the duration so the seq it hands out is
     * unique even when several participants send at the same moment.
     *
     * @throws IllegalArgumentException if the body is missing or blank
     * @throws ForbiddenException       if the caller is not a participant, or -- for a direct chat
     *                                  only -- the two are no longer friends. History stays
     *                                  readable after unfriending but stops accepting messages.
     *                                  Groups are not friendship-gated after creation: one pair
     *                                  falling out should not silently close the room for everyone.
     */
    @Transactional
    public MessageResponse sendMessage(Long conversationId, Long senderId, String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body is required");
        }

        Conversation conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        requireParticipant(conversation, senderId);

        if (conversation.getType() == ConversationType.DIRECT) {
            List<Long> pair = conversation.getParticipants().stream()
                    .map(participant -> participant.getUser().getId())
                    .toList();
            if (pair.size() == 2) {
                requireFriends(pair.get(0), pair.get(1));
            }
        }

        User sender = userService.getUserByIdOrThrow(senderId);
        LocalDateTime now = LocalDateTime.now();

        conversation.setLastSeq(conversation.getLastSeq() + 1);
        conversation.setLastMessageAt(now);
        // You have read what you just wrote. Beyond being obviously true, this is the invariant the
        // unread count depends on: it keeps your own messages from ever sitting above your pointer,
        // so "everything above the pointer" and "everything unread from others" are the same set.
        conversation.applyLastReadSeqFor(senderId, conversation.getLastSeq());
        conversationRepository.save(conversation);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setSeq(conversation.getLastSeq());
        message.setBody(body.trim());
        return new MessageResponse(messageRepository.save(message));
    }

    /**
     * Moves the caller's read pointer up to the message they have actually seen, and returns the
     * conversation so the caller gets the resulting unreadCount without a follow-up request.
     *
     * The pointer only ever moves forward, and never past the newest message that exists. That
     * makes this idempotent and safe to fire on every poll: a duplicate or out-of-order request
     * cannot rewind the position, and a client reporting a seq from a page it rendered before a
     * new message landed marks only what it really showed.
     *
     * @param lastReadSeq the newest seq the caller has seen; null means "everything there is"
     * @throws ForbiddenException if the caller is not a participant
     */
    @Transactional
    public ConversationResponse markRead(Long conversationId, Long viewerId, Long lastReadSeq) {
        Conversation conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        requireParticipant(conversation, viewerId);

        long requested = lastReadSeq == null ? conversation.getLastSeq() : lastReadSeq;
        long capped = Math.min(requested, conversation.getLastSeq());
        long current = conversation.participantFor(viewerId)
                .map(participant -> participant.getLastReadSeq())
                .orElse(0L);

        conversation.applyLastReadSeqFor(viewerId, Math.max(capped, current));
        return new ConversationResponse(conversationRepository.save(conversation), viewerId);
    }

    public Conversation getByIdOrThrow(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    private Conversation getWithParticipantsOrThrow(Long conversationId) {
        return conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    /**
     * The lookup key for a direct chat. Smaller id first, so the pair maps to one key whichever way
     * round it is asked for -- which is what makes getOrCreate idempotent.
     */
    private static String directKeyFor(Long a, Long b) {
        return Math.min(a, b) + ":" + Math.max(a, b);
    }

    private Conversation createDirect(User viewer, User other) {
        Conversation conversation = new Conversation();
        conversation.setType(ConversationType.DIRECT);
        conversation.setDirectKey(directKeyFor(viewer.getId(), other.getId()));
        conversation.setLastSeq(0L);
        conversation.setCreateDate(LocalDateTime.now());
        // Seeded rather than left null so the chat list's ORDER BY never has to deal with nulls.
        conversation.setLastMessageAt(conversation.getCreateDate());
        conversation.addParticipant(viewer, conversation.getCreateDate());
        conversation.addParticipant(other, conversation.getCreateDate());
        return conversationRepository.save(conversation);
    }

    private void requireParticipant(Conversation conversation, Long userId) {
        if (!conversation.hasParticipant(userId)) {
            throw new ForbiddenException("Not your conversation");
        }
    }

    private void requireFriends(Long userId, Long otherUserId) {
        if (!friendRequestService.areFriends(userId, otherUserId)) {
            throw new ForbiddenException("You can only chat with friends");
        }
    }
}
