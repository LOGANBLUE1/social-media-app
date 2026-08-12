package com.example.app.services;

import com.example.app.dataAccess.CommentRepository;
import com.example.app.dataAccess.LikeRepository;
import com.example.app.dataAccess.PostRepository;
import com.example.app.dto.PostCountProjection;
import com.example.app.entities.Post;
import com.example.app.entities.User;
import com.example.app.exceptions.NotFoundException;
import com.example.app.requests.CreatePostRequest;
import com.example.app.requests.UpdatePostRequest;
import com.example.app.responses.PostResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    public PostService(PostRepository postRepository,
                       UserService userService,
                       LikeRepository likeRepository,
                       CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.userService = userService;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * @param viewerId whose like state to report, i.e. the caller -- not the author being listed
     */
    public List<PostResponse> getAllPosts(Long userId, Long viewerId) {
        return toResponses(postRepository.findByUserId(userId), viewerId);
    }

    /**
     * Every post, newest first -- what the homepage renders. There is no follow graph yet, so the
     * feed is global rather than personalised.
     */
    public List<PostResponse> getFeed(Long viewerId) {
        return toResponses(postRepository.findAllByOrderByCreateDateDesc(), viewerId);
    }

    public Post getPostByIdOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));
    }

    /**
     * @throws NotFoundException if postId has no row. Raised by getPostByIdOrThrow.
     */
    public PostResponse getPostByIdWithStats(Long postId, Long viewerId) {
        return withStats(getPostByIdOrThrow(postId), viewerId);
    }

    /**
     * @throws NotFoundException if the request's userId has no row. Raised by
     *                           userService.getUserByIdOrThrow.
     */
    public PostResponse createPost(CreatePostRequest newPostRequest) {
        User user = userService.getUserByIdOrThrow(newPostRequest.getUserId());
        Post post = new Post();
        post.setDescription(newPostRequest.getDescription());
        post.setTitle(newPostRequest.getTitle());
        post.setUser(user);
        post.setCreateDate(LocalDateTime.now());
        // A new post has no likes or comments yet, and its author has not liked it.
        return new PostResponse(postRepository.save(post), 0L, 0L, false);
    }

    /**
     * @throws NotFoundException if postId has no row. Raised by getPostByIdOrThrow.
     */
    public PostResponse updatePostById(Long postId, UpdatePostRequest updatePostRequest, Long viewerId) {
        Post post = getPostByIdOrThrow(postId);
        post.setDescription(updatePostRequest.getText());
        post.setTitle(updatePostRequest.getTitle());
        postRepository.save(post);
        return withStats(post, viewerId);
    }

    public void deletePostById(Long postId) {
        postRepository.deleteById(postId);
    }

    /** Counts for one post: three small queries beat loading its like and comment rows. */
    private PostResponse withStats(Post post, Long viewerId) {
        boolean likedByMe = viewerId != null
                && likeRepository.existsByUserIdAndPostId(viewerId, post.getId());
        return new PostResponse(post,
                likeRepository.countByPostId(post.getId()),
                commentRepository.countByPostId(post.getId()),
                likedByMe);
    }

    /**
     * Counts for a list of posts, resolved in a fixed number of queries however long the list is --
     * one for the like counts, one for the comment counts, one for the viewer's own likes.
     */
    private List<PostResponse> toResponses(List<Post> posts, Long viewerId) {
        if (posts.isEmpty()) {
            return List.of();   // the IN (:postIds) queries below are not valid SQL when empty
        }

        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, Long> likeCounts = toCountMap(likeRepository.countByPostIds(postIds));
        Map<Long, Long> commentCounts = toCountMap(commentRepository.countByPostIds(postIds));
        Set<Long> likedByViewer = viewerId == null
                ? Set.of()
                : new HashSet<>(likeRepository.findLikedPostIds(viewerId, postIds));

        return posts.stream()
                .map(post -> new PostResponse(post,
                        likeCounts.getOrDefault(post.getId(), 0L),
                        commentCounts.getOrDefault(post.getId(), 0L),
                        likedByViewer.contains(post.getId())))
                .collect(Collectors.toList());
    }

    /** Posts with no likes or comments are simply absent from the GROUP BY, hence the defaults above. */
    private static Map<Long, Long> toCountMap(List<PostCountProjection> rows) {
        return rows.stream()
                .collect(Collectors.toMap(PostCountProjection::getPostId, PostCountProjection::getTotal));
    }
}
