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
        return ApiResponse.build(HttpStatus.OK, "Successful", postService.getAllPosts(userId));
    }

    @PostMapping
    public Post createPost(@RequestBody CreatePostRequest newPostRequest) {
        System.out.println("Creating post with data: " + newPostRequest);
        return postService.createPost(newPostRequest);
    }


    @GetMapping("/{postId}")
    public PostResponse getPostById(@PathVariable Long postId) {
//        return postService.getPostById(postId);
        return postService.getPostByIdWithLikes(postId);
    }

    @PutMapping("/{postId}")
    public Post updatePostById(@PathVariable Long postId, @RequestBody UpdatePostRequest updatePostRequest){
        return postService.updatePostById(postId, updatePostRequest);
    }

    @DeleteMapping("/{postId}")
    public void deletePostById(@PathVariable Long postId){
        postService.deletePostById(postId);
    }

}
