package com.example.app.requests;

import lombok.Data;

/**
 * The conversation is in the path and the sender comes from the principal, so the body carries only
 * what the client actually owns.
 */
@Data
public class CreateMessageRequest {
    private String body;
}
