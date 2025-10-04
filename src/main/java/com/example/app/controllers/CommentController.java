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
    public List<CommentResponse> getAllComments(@RequestParam Optional<Long> userId, @RequestParam Optional<Long> postId) {
        return commentService.getAllComments(userId, postId);
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

    @GetMapping("/{commentId}")
    public ResponseEntity<?> getCommentById(@PathVariable Long commentId){
        if(commentId == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "commentId is required", null);
        }
        var res = commentService.getCommentById(commentId);
        if(res==null){
            return ApiResponse.build(HttpStatus.NOT_FOUND, "Comment not found", null);
        }
        return ApiResponse.success(res);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateCommentById(@PathVariable Long commentId, @RequestBody UpdateCommentRequest updateCommentRequest){
        if(commentId == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "commentId is required", null);
        }
        var res = commentService.updateCommentById(commentId, updateCommentRequest);
        if(res==null){
            return ApiResponse.build(HttpStatus.NOT_FOUND, "Comment not found", null);
        }
        return ApiResponse.success(res);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteCommentById(@PathVariable Long commentId){
        System.out.println("Deleting comment with ID: " + commentId);
        if(commentId == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "commentId is required", null);
        }
        commentService.deleteCommentById(commentId);
        return ApiResponse.build(HttpStatus.NO_CONTENT, null, null);
    }
}
