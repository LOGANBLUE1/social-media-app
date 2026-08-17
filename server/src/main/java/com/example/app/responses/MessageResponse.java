package com.example.app.responses;

import com.example.app.entities.Message;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One message. Carries the sender's name as well as their id: in a group, "who said this" cannot
 * be inferred from position or from the two-people-in-the-room assumption a direct chat allows.
 * Direct chats ignore it and keep rendering by ownership.
 */
@Data
public class MessageResponse {
    private Long id;
    /** Position in the conversation. Sort on this, not createdAt. */
    private Long seq;
    private Long senderId;
    /** Rendered above the bubble in group chats. */
    private String senderUsername;
    private String body;
    private LocalDateTime createdAt;

    public MessageResponse(Message message) {
        this.id = message.getId();
        this.seq = message.getSeq();
        this.senderId = message.getSender().getId();
        // Unlike the id, this does initialise the lazy sender proxy. The alternative -- resolving
        // names client-side from the participant list -- breaks for anyone who has left the room.
        this.senderUsername = message.getSender().getUsername();
        this.body = message.getBody();
        this.createdAt = message.getCreateDate();
    }
}
