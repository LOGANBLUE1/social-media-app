package com.example.app.controllers;


import com.example.app.entities.Like;
import com.example.app.requests.CreateLikeRequest;
import com.example.app.responses.LikeResponse;
import com.example.app.services.LikeService;
import com.example.app.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @GetMapping
    public ResponseEntity<?> getPostLikes(@RequestParam Long postId) {
        if(postId == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "postId is required", null);
        }
        return ApiResponse.success(likeService.getPostLikes(postId));
    }
//    @GetMapping
//    public ResponseEntity<?> getUserLikes(@RequestParam Long userId) {
//        if(userId == null){
//            return ApiResponse.build(HttpStatus.BAD_REQUEST, "userId is required", null);
//        }
//        return ApiResponse.success(likeService.getUserLikes(userId));
//    }

    @PostMapping
    public ResponseEntity<?> createLike(@RequestBody CreateLikeRequest createLikeRequest) {
        return ApiResponse.created(likeService.createLike(createLikeRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLikeById(@PathVariable Long id) {
        if(id == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "likeId is required", null);
        }
        return ApiResponse.success(likeService.getLikeById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLikeById(@PathVariable Long id) {
        if(id == null){
            return ApiResponse.build(HttpStatus.BAD_REQUEST, "likeId is required", null);
        }
        likeService.deleteLikeById(id);
        return ApiResponse.deleted();
    }
}
