package com.example.app.services;

import com.example.app.dataAccess.LikeRepository;
import com.example.app.entities.Like;
import com.example.app.entities.Post;
import com.example.app.entities.User;
import com.example.app.exceptions.NotFoundException;
import com.example.app.requests.CreateLikeRequest;
import com.example.app.responses.LikeResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LikeService {
    private final LikeRepository likeRepository;
    private final UserService userService;
    private final PostService postService;

    public LikeService(LikeRepository likeRepository, UserService userService, PostService postService) {
        this.likeRepository = likeRepository;
        this.userService = userService;
        this.postService = postService;
    }

    public List<LikeResponse> getPostLikes(Long postId) {
        List<Like> list = likeRepository.findByPostId(postId);
        return list.stream().map(LikeResponse::new).collect(Collectors.toList());
    }

    public List<LikeResponse> getUserLikes(Long userId) {
        List<Like> list = likeRepository.findByUserId(userId);
        return list.stream().map(LikeResponse::new).collect(Collectors.toList());
    }

    public Like getLikeById(Long LikeId) {
        return likeRepository.findById(LikeId)
                .orElseThrow(() -> new NotFoundException("Like not found"));
    }

    public Like createLike(CreateLikeRequest CreateLikeRequest) {
        User user = userService.getUserByIdOrThrow(CreateLikeRequest.getUserId());
        Post post = postService.getPostByIdOrThrow(CreateLikeRequest.getPostId());
        if(user != null && post != null) {
            Like like = new Like();
            like.setPost(post);
            like.setUser(user);
            return likeRepository.save(like);
        }else
            return null;
    }

    public void deleteLikeById(Long likeId) {
        likeRepository.deleteById(likeId);
    }

}
