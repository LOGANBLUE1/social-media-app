package com.example.app.services;

import com.example.app.dataAccess.PostRepository;
import com.example.app.entities.Like;
import com.example.app.entities.Post;
import com.example.app.entities.User;
import com.example.app.requests.CreatePostRequest;
import com.example.app.requests.UpdatePostRequest;
import com.example.app.responses.LikeResponse;
import com.example.app.responses.PostResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private LikeService likeService;


    public PostService(PostRepository postRepository, UserService userService) {
        this.postRepository = postRepository;
        this.userService = userService;
    }

    @Autowired
    public void setLikeService(@Lazy LikeService likeService) {
        this.likeService = likeService;
    }

    public List<PostResponse> getAllPosts(Optional<Long> userId) {
        List<Post> postList = userId
                .map(postRepository::findByUserId)
                .orElseGet(postRepository::findAll);

        return postList.stream()
                .map(post -> {
                    List<LikeResponse> likes = likeService.getAllLikes(Optional.empty(), Optional.of(post.getId()));
                    return new PostResponse(post, likes);
                })
                .toList();
    }

    public Post getPostById(Long postId) {
        System.out.println("PostService - getPostById called with postId: " + postId);
        return postRepository.findById(postId).orElse(null);
    }

    public PostResponse getPostByIdWithLikes(Long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        List<LikeResponse> likes = likeService.getAllLikes(Optional.empty(), Optional.of(postId));
        if (post != null) {
            return new PostResponse(post, likes);
        } else {
            return null;
        }
    }

    public Post createPost(CreatePostRequest newPostRequest) {
        User user = userService.getUserById(newPostRequest.getUserId());
        if(user != null) {
            Post post = new Post();
            post.setText(newPostRequest.getText());
            post.setTitle(newPostRequest.getTitle());
            post.setUser(user);
            post.setCreateDate(LocalDateTime.now());
            return postRepository.save(post);
        } else {
            return null;
        }
    }

    public Post updatePostById(Long postId, UpdatePostRequest updatePostRequest) {
        Post post = postRepository.findById(postId).orElse(null);
        if(post != null) {
            post.setText(updatePostRequest.getText());
            post.setTitle(updatePostRequest.getTitle());
            postRepository.save(post);
            return post;
        }
        return null;
    }

    public void deletePostById(Long postId) {
        postRepository.deleteById(postId);
    }
}
