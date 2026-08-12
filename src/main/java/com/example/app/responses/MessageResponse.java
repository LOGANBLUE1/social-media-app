package com.example.app.responses;

import com.example.app.entities.Message;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Only the sender's id, not the whole user: a 1:1 chat has two participants and the client already
 * knows both from the conversation, so repeating a username on every message is wasted payload.
 */
@Data
public class MessageResponse {
    private Long id;
    /** Position in the conversation. Sort on this, not createdAt. */
    private Long seq;
    private Long senderId;
    private String body;
    private LocalDateTime createdAt;

    public MessageResponse(Message message) {
        this.id = message.getId();
        this.seq = message.getSeq();
        // Reading the id off a lazy proxy does not trigger a load, so this stays one query.
        this.senderId = message.getSender().getId();
        this.body = message.getBody();
        this.createdAt = message.getCreateDate();
    }
}
