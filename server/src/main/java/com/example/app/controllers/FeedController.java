package com.example.app.controllers;

import com.example.app.responses.PostResponse;
import com.example.app.security.JWTUserDetails;
import com.example.app.services.PostService;
import com.example.app.utils.Response;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The homepage. Mapped at the root so a client hitting the server with no path lands on the feed,
 * with /feed kept as an explicit alias.
 */
@RestController
public class FeedController {
    private final PostService postService;

    public FeedController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping({"/", "/feed"})
    public Response<List<PostResponse>> getFeed(@AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(postService.getFeed(user.getId()));
    }
}