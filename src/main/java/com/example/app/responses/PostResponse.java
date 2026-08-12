package com.example.app.responses;


import com.example.app.entities.Post;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A post as any list or detail view renders it: aggregate counts plus the viewer's own like state,
 * rather than the like rows themselves. The full list of likers is unbounded, so it is fetched on
 * demand from GET /likes?postId= instead of being embedded here.
 */
@Data
public class PostResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private UserResponse author;
    private long likeCount;
    private long commentCount;
    /** Whether the user making the request has liked this post -- drives the like button's state. */
    private boolean likedByMe;

    public PostResponse(Post post, long likeCount, long commentCount, boolean likedByMe) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.description = post.getDescription();
        this.createdAt = post.getCreateDate();
        this.author = new UserResponse(post.getUser());
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.likedByMe = likedByMe;
    }
}
