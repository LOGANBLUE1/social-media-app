package com.example.app.controllers;


import com.example.app.entities.Comment;
import com.example.app.requests.CreateCommentRequest;
import com.example.app.requests.UpdateCommentRequest;
import com.example.app.responses.CommentResponse;
import com.example.app.security.JWTUserDetails;
import com.example.app.services.CommentService;
import com.example.app.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<?> getAllComments(@RequestParam Optional<Long> userId, @RequestParam Optional<Long> postId) {
        return ApiResponse.success(commentService.getAllComments(userId, postId));
    }

    @PostMapping
    public ResponseEntity<?> createComment(@RequestBody CreateCommentRequest createCommentRequest, @AuthenticationPrincipal JWTUserDetails user){
        createCommentRequest.setUserId(user.getId());
        var res = commentService.createComment(createCommentRequest);
        if(res==null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "userId and postId are required", null);
        }
        return ApiResponse.build(HttpStatus.CREATED, "success", res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCommentById(@PathVariable Long id){
        if(id == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "id is required", null);
        }
        var res = commentService.getCommentByIdOrThrow(id);
        return ApiResponse.success(res);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCommentById(@PathVariable Long id, @RequestBody UpdateCommentRequest updateCommentRequest){
        if(id == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "id is required", null);
        }
        var res = commentService.updateCommentById(id, updateCommentRequest);
        return ApiResponse.success(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCommentById(@PathVariable Long id){
        if(id == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "commentId is required", null);
        }
        commentService.deleteCommentById(id);
        return ApiResponse.deleted();
    }
}
