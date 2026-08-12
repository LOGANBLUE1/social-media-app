package com.example.app.requests;
import lombok.Data;

@Data
public class CreatePostRequest {
    private Long userId;
    private String title;
    private String description;
}
