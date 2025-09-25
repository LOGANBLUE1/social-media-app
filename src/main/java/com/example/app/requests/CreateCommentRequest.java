package com.example.app.requests;

import lombok.Data;

@Data
public class CreateCommentRequest {

    private Long id;
    private Long postId;
    private Long userId;
    private String text;
}
