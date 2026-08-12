package com.example.app.requests;

import lombok.Data;

/** The other participant. The caller is taken from the principal. */
@Data
public class CreateConversationRequest {
    private Long userId;
}
