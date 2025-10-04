package com.example.app.services;


import com.example.app.dataAccess.CommentRepository;
import com.example.app.dataAccess.LikeRepository;
import com.example.app.dataAccess.PostRepository;
import com.example.app.dataAccess.UserRepository;
import com.example.app.dto.UserCommentProjection;
import com.example.app.dto.UserLikeProjection;
import com.example.app.entities.User;
import com.example.app.requests.UserRequest;
import com.example.app.responses.UserActivityResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;


    public UserService(UserRepository userRepository,
                       LikeRepository likeRepository,
                       CommentRepository commentRepository,
                       PostRepository postRepository) {
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public User updateUserById(Long userId, UserRequest newUser) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()){  //user var mi
            User foundUser = user.get();
            foundUser.setUsername(newUser.getUsername());
            foundUser.setPassword(newUser.getPassword());
            foundUser.setImage(newUser.getImage());
            userRepository.save(foundUser);
            return foundUser;
        } else {
            return null;
        }
    }

    public void deleteUserById(Long userId) {
        userRepository.deleteById(userId);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public UserActivityResponse getUserActivityById(Long userId) {
        List<Long> postIds = postRepository.findTop5ByUserId(userId);
        if(postIds.isEmpty()) {
            return null;
        }
        var response = new UserActivityResponse();
        List<UserCommentProjection> comments = commentRepository.findUserCommentsByPostId(postIds);
        List<UserLikeProjection> likes = likeRepository.findUserLikesByPostId(postIds);
        response.setComments(comments);
        response.setLikes(likes);
        return response;
    }
}
