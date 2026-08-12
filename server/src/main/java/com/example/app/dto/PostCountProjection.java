package com.example.app.dto;

/**
 * One aggregate row per post -- how many likes, or how many comments, it has. Lets a list of posts
 * resolve its counts in a single query instead of one per post.
 */
public interface PostCountProjection {
    Long getPostId();

    Long getTotal();
}
