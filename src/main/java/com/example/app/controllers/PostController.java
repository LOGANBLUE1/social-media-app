package com.example.app.controllers;

import com.example.app.entities.Post;
import com.example.app.requests.CreatePostRequest;
import com.example.app.requests.UpdatePostRequest;
import com.example.app.responses.PostResponse;
import com.example.app.services.PostService;
import com.example.app.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping  // RequestParam =>  posts?userId=userId
    public ResponseEntity<?> getAllPosts(@RequestParam Optional<Long> userId) {
        return ApiResponse.success(postService.getAllPosts(userId));
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody CreatePostRequest newPostRequest) {
        return ApiResponse.created(postService.createPost(newPostRequest));
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Long id) {
        if(id == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "postId is required", null);
        }
        return ApiResponse.success(postService.getPostByIdWithLikes(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePostById(@PathVariable Long id, @RequestBody UpdatePostRequest updatePostRequest){
        if(id == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "postId is required", null);
        }
        return ApiResponse.success(postService.updatePostById(id, updatePostRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePostById(@PathVariable Long id){
        if(id == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "postId is required", null);
        }
        postService.deletePostById(id);
        return ApiResponse.deleted();
    }
}
