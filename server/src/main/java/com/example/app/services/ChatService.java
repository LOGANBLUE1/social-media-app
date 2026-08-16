package com.example.app.services;

import com.example.app.dataAccess.ConversationRepository;
import com.example.app.dataAccess.MessageRepository;
import com.example.app.entities.Conversation;
import com.example.app.entities.Message;
import com.example.app.entities.User;
import com.example.app.exceptions.ForbiddenException;
import com.example.app.exceptions.NotFoundException;
import com.example.app.responses.ConversationResponse;
import com.example.app.responses.MessageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

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
     * Opens the chat with another user, creating it on first use. Idempotent: asking twice returns
     * the same conversation, because the stored pair is normalised.
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
        userService.getUserByIdOrThrow(otherUserId);
        requireFriends(viewerId, otherUserId);

        Long lowId = Math.min(viewerId, otherUserId);
        Long highId = Math.max(viewerId, otherUserId);

        Conversation conversation = conversationRepository
                .findByUserLowIdAndUserHighId(lowId, highId)
                .orElseGet(() -> create(lowId, highId));

        return new ConversationResponse(conversation, viewerId);
    }

    /** The caller's chats, most recently active first. */
    public List<ConversationResponse> listConversations(Long viewerId) {
        return conversationRepository.findByParticipant(viewerId).stream()
                .map(conversation -> new ConversationResponse(conversation, viewerId))
                .collect(Collectors.toList());
    }

    /**
     * The entire conversation, oldest message first.
     *
     * @throws ForbiddenException if the caller is not one of the two participants
     */
    public List<MessageResponse> listMessages(Long conversationId, Long viewerId) {
        requireParticipant(getByIdOrThrow(conversationId), viewerId);
        return messageRepository.findByConversationIdOrderBySeqAsc(conversationId).stream()
                .map(MessageResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Appends a message. The conversation row is locked for the duration so the seq it hands out is
     * unique even when both participants send at the same moment.
     *
     * @throws IllegalArgumentException if the body is missing or blank
     * @throws ForbiddenException       if the caller is not a participant, or they are no longer
     *                                  friends -- history stays readable after unfriending, but
     *                                  the conversation stops accepting new messages
     */
    @Transactional
    public MessageResponse sendMessage(Long conversationId, Long senderId, String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body is required");
        }

        Conversation conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        requireParticipant(conversation, senderId);
        requireFriends(conversation.getUserLow().getId(), conversation.getUserHigh().getId());

        User sender = userService.getUserByIdOrThrow(senderId);
        LocalDateTime now = LocalDateTime.now();

        conversation.setLastSeq(conversation.getLastSeq() + 1);
        conversation.setLastMessageAt(now);
        // You have read what you just wrote. Beyond being obviously true, this is the invariant the
        // unread count depends on: it keeps your own messages from ever sitting above your pointer,
        // so "everything above the pointer" and "everything unread from them" are the same set.
        conversation.applyLastReadSeqFor(senderId, conversation.getLastSeq());
        conversationRepository.save(conversation);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setSeq(conversation.getLastSeq());
        message.setBody(body.trim());
        message.setCreateDate(now);
        return new MessageResponse(messageRepository.save(message));
    }

    /**
     * Moves the caller's read pointer up to the message they have actually seen, and returns the
     * conversation so the caller gets the resulting unreadCount without a follow-up request.
     *
     * The pointer only ever moves forward, and never past the newest message that exists. That
     * makes this idempotent and safe to fire on every poll: a duplicate or out-of-order request
     * cannot rewind the position, and a client that reports a seq from a page it rendered before
     * a new message landed marks only what it really showed.
     *
     * Takes the row lock so a send racing this cannot have its seq clamped away by a stale read
     * of lastSeq.
     *
     * @param lastReadSeq the newest seq the caller has seen; null means "everything there is"
     * @throws ForbiddenException if the caller is not one of the two participants
     */
    @Transactional
    public ConversationResponse markRead(Long conversationId, Long viewerId, Long lastReadSeq) {
        Conversation conversation = conversationRepository.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        requireParticipant(conversation, viewerId);

        long requested = lastReadSeq == null ? conversation.getLastSeq() : lastReadSeq;
        long capped = Math.min(requested, conversation.getLastSeq());
        long target = Math.max(capped, conversation.lastReadSeqFor(viewerId));

        conversation.applyLastReadSeqFor(viewerId, target);
        return new ConversationResponse(conversationRepository.save(conversation), viewerId);
    }

    public Conversation getByIdOrThrow(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    private Conversation create(Long lowId, Long highId) {
        Conversation conversation = new Conversation();
        conversation.setUserLow(userService.getUserByIdOrThrow(lowId));
        conversation.setUserHigh(userService.getUserByIdOrThrow(highId));
        conversation.setLastSeq(0L);
        conversation.setCreateDate(LocalDateTime.now());
        // Seeded rather than left null so the chat list's ORDER BY never has to deal with nulls.
        conversation.setLastMessageAt(conversation.getCreateDate());
        return conversationRepository.save(conversation);
    }

    private void requireParticipant(Conversation conversation, Long userId) {
        boolean participant = conversation.getUserLow().getId().equals(userId)
                || conversation.getUserHigh().getId().equals(userId);
        if (!participant) {
            throw new ForbiddenException("Not your conversation");
        }
    }

    private void requireFriends(Long userId, Long otherUserId) {
        if (!friendRequestService.areFriends(userId, otherUserId)) {
            throw new ForbiddenException("You can only chat with friends");
        }
    }
}
