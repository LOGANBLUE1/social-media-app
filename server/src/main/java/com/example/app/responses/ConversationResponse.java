package com.example.app.responses;

import com.example.app.entities.Conversation;
import com.example.app.entities.ConversationParticipant;
import com.example.app.entities.ConversationType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A conversation from one participant's point of view.
 *
 * Both shapes go over the same DTO rather than splitting into two, so the client keeps one chat
 * list: `type` says which fields are meaningful. A direct chat fills otherUser and leaves name
 * null; a group does the reverse. `participants` is always populated -- a group renders its member
 * list from it, and a direct chat can resolve a senderId to a name with it.
 */
@Data
public class ConversationResponse {
    private Long id;
    private ConversationType type;
    /** Group title. Null for a direct chat, which is named after the other person. */
    private String name;
    /** The other person in a direct chat. Null for a group, which has no single "other". */
    private UserResponse otherUser;
    /** Everyone in the room, the viewer included. */
    private List<UserResponse> participants;
    private LocalDateTime lastMessageAt;
    /** How many messages the conversation holds -- also the seq of its newest message. */
    private Long lastSeq;
    /** Messages the viewer has not read yet. */
    private Long unreadCount;

    public ConversationResponse(Conversation conversation, Long viewerId) {
        this.id = conversation.getId();
        this.type = conversation.getType();
        this.name = conversation.getName();
        this.otherUser = conversation.otherParticipant(viewerId).map(UserResponse::new).orElse(null);
        this.participants = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .map(UserResponse::new)
                .toList();
        this.lastMessageAt = conversation.getLastMessageAt();
        this.lastSeq = conversation.getLastSeq();
        this.unreadCount = conversation.unreadCountFor(viewerId);
    }
}
