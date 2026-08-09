package com.example.app.controllers;

import com.example.app.entities.Post;
import com.example.app.requests.CreatePostRequest;
import com.example.app.requests.UpdatePostRequest;
import com.example.app.responses.PostResponse;
import com.example.app.security.JWTUserDetails;
import com.example.app.services.PostService;
import com.example.app.utils.Response;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/me")
    public Response<List<PostResponse>> getAllPosts(@AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(postService.getAllPosts(user.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response<PostResponse> createPost(@RequestBody CreatePostRequest newPostRequest) {
        return Response.success(postService.createPost(newPostRequest));
    }

    @GetMapping("/{id}")
    public Response<PostResponse> getPostById(@PathVariable Long id) {
        return Response.success(postService.getPostByIdWithLikes(id));
    }

    @PutMapping("/{id}")
    public Response<PostResponse> updatePostById(@PathVariable Long id, @RequestBody UpdatePostRequest updatePostRequest) {
        return Response.success(postService.updatePostById(id, updatePostRequest));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePostById(@PathVariable Long id) {
        postService.deletePostById(id);
    }
}