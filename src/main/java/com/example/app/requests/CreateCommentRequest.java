package com.example.app.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateCommentRequest {
    private Long postId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long userId;
    private String text;
}
