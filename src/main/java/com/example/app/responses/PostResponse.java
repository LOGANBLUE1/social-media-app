package com.example.app.responses;


import com.example.app.entities.Post;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private List<LikeResponse> likes; // optional, can be null if not needed

    public PostResponse(Post post, List<LikeResponse> likes) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.description = post.getDescription();
        this.likes = likes;
        this.createdAt = post.getCreateDate();
    }
}
