package com.example.app.services;


import com.example.app.dataAccess.CommentRepository;
import com.example.app.dataAccess.LikeRepository;
import com.example.app.dataAccess.PostRepository;
import com.example.app.dataAccess.UserRepository;
import com.example.app.dto.UserCommentProjection;
import com.example.app.dto.UserLikeProjection;
import com.example.app.entities.User;
import com.example.app.exceptions.NotFoundException;
import com.example.app.requests.UserRequest;
import com.example.app.responses.UserActivityResponse;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * @throws NotFoundException if userId has no row
     */
    public User getUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Profile fields only. Credentials are AuthService's concern -- any password in the
     * request body is ignored; changing one goes through POST /auth/change-password.
     *
     * @throws NotFoundException if userId has no row. Raised by getUserByIdOrThrow.
     */
    public User updateUserById(Long userId, UserRequest newUser) {
        User user = getUserByIdOrThrow(userId);

        user.setUsername(newUser.getUsername());
        user.setImage(newUser.getImage());
        userRepository.save(user);
        return user;
    }

    public void deleteUserById(Long userId) {
        userRepository.deleteById(userId);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * @return null when the user has no posts -- callers decide what that means for the response
     */
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
