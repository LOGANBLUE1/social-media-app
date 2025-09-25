package com.example.app.responses;

import com.example.app.entities.Like;
import lombok.Data;

@Data
public class LikeResponse {  //user ve postun hepsini dönmek yerine sadece Id lerini dönücez bu yüzden LikeResponse oluşturduk.

    private Long id;
    private Long userId;
    private Long postId;

    public LikeResponse(Like like) {
        this.id = like.getId();
        this.userId = like.getUser().getId();
        this.postId = like.getPost().getId();
    }
}
