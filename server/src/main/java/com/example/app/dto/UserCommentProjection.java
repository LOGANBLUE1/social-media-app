package com.example.app.dto;

public interface UserCommentProjection {
    Long getPostId();
    String getText();
    String getUsername();
    String getImage();
}