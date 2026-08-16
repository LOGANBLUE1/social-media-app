package com.example.app.responses;

import com.example.app.entities.Conversation;
import com.example.app.entities.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A conversation from one participant's point of view: the person they are talking to, rather than
 * the raw low/high pair, which is a storage detail.
 */
@Data
public class ConversationResponse {
    private Long id;
    private UserResponse otherUser;
    private LocalDateTime lastMessageAt;
    /** How many messages the conversation holds -- also the seq of its newest message. */
    private Long lastSeq;
    /** Messages from the other person that the viewer has not read yet. */
    private Long unreadCount;

    public ConversationResponse(Conversation conversation, Long viewerId) {
        User other = conversation.getUserLow().getId().equals(viewerId)
                ? conversation.getUserHigh()
                : conversation.getUserLow();

        this.id = conversation.getId();
        this.otherUser = new UserResponse(other);
        this.lastMessageAt = conversation.getLastMessageAt();
        this.lastSeq = conversation.getLastSeq();
        this.unreadCount = conversation.unreadCountFor(viewerId);
    }
}
