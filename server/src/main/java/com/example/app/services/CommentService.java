package com.example.app.services;


import com.example.app.dataAccess.CommentRepository;
import com.example.app.entities.Comment;
import com.example.app.entities.Post;
import com.example.app.entities.User;
import com.example.app.exceptions.NotFoundException;
import com.example.app.requests.CreateCommentRequest;
import com.example.app.requests.UpdateCommentRequest;
import com.example.app.responses.CommentResponse;
import org.springframework.stereotype.Service;

import java.util.List;

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

    /** Every comment on one post -- what the post detail view renders. */
    public List<CommentResponse> getAllPostComments(Long postId) {
        return toResponses(commentRepository.findByPostId(postId));
    }

    /**
     * Comments filtered by author, by post, or by both. A null id means "no constraint on this
     * field", so the three combinations map onto the three derived queries.
     *
     * Passing neither returns nothing rather than the whole table. An unfiltered dump grows without
     * bound and no caller has ever wanted one -- making it the default for a missing parameter would
     * turn a forgotten query string into a full table scan.
     */
    public List<CommentResponse> getAllComments(Long userId, Long postId) {
        if (userId != null && postId != null) {
            return toResponses(commentRepository.findByUserIdAndPostId(userId, postId));
        }
        else if (userId != null) {
            return toResponses(commentRepository.findByUserId(userId));
        }
        else if (postId != null) {
            return toResponses(commentRepository.findByPostId(postId));
        }
        return List.of();
    }

    public Comment getCommentByIdOrThrow(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
    }

    /**
     * @throws NotFoundException if the request's userId or postId has no row. Raised by
     *                           userService.getUserByIdOrThrow / postService.getPostByIdOrThrow.
     */
    public Comment createComment(CreateCommentRequest request) {
        User user = userService.getUserByIdOrThrow(request.getUserId());
        Post post = postService.getPostByIdOrThrow(request.getPostId());

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setText(request.getText());
        return commentRepository.save(comment);
    }

    /**
     * @throws NotFoundException if id has no row. Raised by getCommentByIdOrThrow.
     */
    public Comment updateCommentById(Long id, UpdateCommentRequest request) {
        Comment comment = this.getCommentByIdOrThrow(id);
        comment.setText(request.getText());
        return commentRepository.save(comment);
    }

    public void deleteCommentById(Long id) {
        commentRepository.deleteById(id);
    }

    private static List<CommentResponse> toResponses(List<Comment> comments) {
        return comments.stream().map(CommentResponse::new).toList();
    }
}

