package com.example.app.responses;

import com.example.app.entities.Comment;
import lombok.Data;

@Data
public class CommentResponse {

    private Long id;
    private Long userId;
    private String text;

    private String username;

    public CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.userId = comment.getUser().getId();
        this.username = comment.getUser().getName();
        this.text = comment.getText();
    }
}
