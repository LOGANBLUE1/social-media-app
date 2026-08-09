package com.example.app.controllers;


import com.example.app.entities.Like;
import com.example.app.requests.CreateLikeRequest;
import com.example.app.responses.LikeResponse;
import com.example.app.services.LikeService;
import com.example.app.utils.Response;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @GetMapping
    public Response<List<LikeResponse>> getPostLikes(@RequestParam Long postId) {
        return Response.success(likeService.getPostLikes(postId));
    }
//    @GetMapping
//    public Response<List<LikeResponse>> getUserLikes(@RequestParam Long userId) {
//        return Response.success(likeService.getUserLikes(userId));
//    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Like> createLike(@RequestBody CreateLikeRequest createLikeRequest) {
        return Response.success(likeService.createLike(createLikeRequest));
    }

    @GetMapping("/{id}")
    public Response<Like> getLikeById(@PathVariable Long id) {
        return Response.success(likeService.getLikeById(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLikeById(@PathVariable Long id) {
        likeService.deleteLikeById(id);
    }
}