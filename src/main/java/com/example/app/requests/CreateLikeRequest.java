package com.example.app.requests;

import lombok.Data;

@Data
public class CreateLikeRequest {
    private Long userId;
    private Long postId;
}