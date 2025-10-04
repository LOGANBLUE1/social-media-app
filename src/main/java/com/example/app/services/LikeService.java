package com.example.app.services;

import com.example.app.dataAccess.LikeRepository;
import com.example.app.entities.Like;
import com.example.app.entities.Post;
import com.example.app.entities.User;
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

    public List<LikeResponse> getAllLikes(Optional<Long> userId, Optional<Long> postId) {
        List<Like> list;
        if(userId.isPresent() && postId.isPresent()) {
            list = likeRepository.findByUserIdAndPostId(userId.get(), postId.get());
        }else if(userId.isPresent()) {
            list = likeRepository.findByUserId(userId.get());
        }else if(postId.isPresent()) {
            list = likeRepository.findByPostId(postId.get());
        }else
            list = likeRepository.findAll();
        return list.stream().map(like -> new LikeResponse(like)).collect(Collectors.toList());  //Like' ları alıp LikeResponse' a mapledik.
    }

    public Like getLikeById(Long LikeId) {
        return likeRepository.findById(LikeId).orElse(null);
    }

    public Like createLike(CreateLikeRequest CreateLikeRequest) {
        User user = userService.getUserById(CreateLikeRequest.getUserId());
        Post post = postService.getPostById(CreateLikeRequest.getPostId());
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
