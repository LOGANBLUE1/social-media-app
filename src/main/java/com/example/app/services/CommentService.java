package com.example.app.services;


import com.example.app.dataAccess.CommentRepository;
import com.example.app.entities.Comment;
import com.example.app.entities.Post;
import com.example.app.entities.User;
import com.example.app.requests.CreateCommentRequest;
import com.example.app.requests.UpdateCommentRequest;
import com.example.app.responses.CommentResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserService userService;
    private final PostService postService;

    public CommentService(CommentRepository commentRepository,
                          UserService userService,
                          PostService postService) {
        this.commentRepository = commentRepository;
        this.userService = userService;
        this.postService = postService;
    }

    public List<CommentResponse> getAllComments(Optional<Long> userId, Optional<Long> postId) {
        List<Comment> comments;
        if(userId.isPresent() && postId.isPresent()) {
            comments = commentRepository.findByUserIdAndPostId(userId.get(), postId.get());  //get diyince içerisindeki değeri alırız.
        } else if(userId.isPresent()){
            comments =  commentRepository.findByUserId(userId.get());
        } else if (postId.isPresent()){
            comments = commentRepository.findByPostId(postId.get());
        } else {
            comments = commentRepository.findAll();
        }
        return comments.stream().map(CommentResponse::new).collect(Collectors.toList());
    }

    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId).orElse(null);
    }

    public Comment createComment(CreateCommentRequest request) {
        System.out.println(request.toString());
        User user = userService.getUserById(request.getUserId());
        Post post = postService.getPostById(request.getPostId());
        if(user != null && post !=null) {
            Comment comment = new Comment();
            comment.setPost(post);
            comment.setUser(user);
            comment.setText(request.getText());
            comment.setCreateDate(LocalDateTime.now());
            return commentRepository.save(comment);
        }
        return null;
    }

    public Comment updateCommentById(Long commentId, @RequestBody UpdateCommentRequest request) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if(comment != null){
            comment.setText(request.getText());
            return commentRepository.save(comment);
        } else {
            return null;
        }
    }

    public void deleteCommentById(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}

