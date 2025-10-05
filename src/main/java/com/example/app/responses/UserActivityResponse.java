package com.example.app.responses;

import com.example.app.dto.UserCommentProjection;
import com.example.app.dto.UserLikeProjection;
import lombok.Data;

import java.util.List;

@Data
public class UserActivityResponse {
    private List<UserCommentProjection> comments;
    private List<UserLikeProjection> likes;
}