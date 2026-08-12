package com.example.app.controllers;


import com.example.app.entities.Comment;
import com.example.app.requests.CreateCommentRequest;
import com.example.app.requests.UpdateCommentRequest;
import com.example.app.responses.CommentResponse;
import com.example.app.security.JWTUserDetails;
import com.example.app.services.CommentService;
import com.example.app.utils.Response;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

//    @GetMapping
//    public Response<List<CommentResponse>> getAllComments(@RequestParam Optional<Long> userId, @RequestParam Optional<Long> postId) {
//        return Response.success(commentService.getAllComments(userId, postId));
//    }

    @GetMapping
    public Response<List<CommentResponse>> getAllPostComments(@RequestParam Long postId) {
        return Response.success(commentService.getAllPostComments(postId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response<CommentResponse> createComment(@RequestBody CreateCommentRequest createCommentRequest,
                                              @AuthenticationPrincipal JWTUserDetails user) {
        createCommentRequest.setUserId(user.getId());
        Comment comment = commentService.createComment(createCommentRequest);
        if (comment == null) {
            throw new IllegalArgumentException("userId and postId are required");
        }
        return Response.success(new CommentResponse(comment), "success");
    }

    @GetMapping("/{id}")
    public Response<CommentResponse> getCommentById(@PathVariable Long id) {
        return Response.success(new CommentResponse(commentService.getCommentByIdOrThrow(id)));
    }

    @PutMapping("/{id}")
    public Response<CommentResponse> updateCommentById(@PathVariable Long id, @RequestBody UpdateCommentRequest updateCommentRequest) {
        return Response.success(new CommentResponse(commentService.updateCommentById(id, updateCommentRequest)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentById(@PathVariable Long id) {
        commentService.deleteCommentById(id);
    }
}