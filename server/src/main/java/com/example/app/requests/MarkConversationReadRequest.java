package com.example.app.requests;

import lombok.Data;

/**
 * How far the caller has read. The conversation is in the path and the reader comes from the
 * principal, so the body carries only the position.
 *
 * The client sends the newest seq it actually rendered rather than leaving this null ("everything"),
 * so a message arriving between the render and this request is not marked read unseen. Null is
 * still accepted and means "whatever exists now".
 */
@Data
public class MarkConversationReadRequest {
    private Long lastReadSeq;
}
