package com.example.app.dataAccess;

import com.example.app.dto.PostCountProjection;
import com.example.app.dto.UserCommentProjection;
import com.example.app.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByUserIdAndPostId(Long userId, Long postId);

    List<Comment> findByUserId(Long userId);

    List<Comment> findByPostId(Long postId);

    long countByPostId(Long postId);

    @Query(value = """
    SELECT post_id AS postId, COUNT(*) AS total
    FROM comment
    WHERE post_id IN :postIds
    GROUP BY post_id
    """, nativeQuery = true)
    List<PostCountProjection> countByPostIds(@Param("postIds") List<Long> postIds);

    @Query(value = """
    SELECT c.post_id AS postId,
           c.text AS text,
           u.image AS image,
           u.username AS username
    FROM comment c
    LEFT JOIN users u ON u.id = c.user_id
    WHERE c.post_id IN :postIds
    LIMIT 5
    """, nativeQuery = true)
    List<UserCommentProjection> findUserCommentsByPostId(@Param("postIds") List<Long> postIds);
}
/* @Query(value = "select * from comment where post_id in :postIds limit 5", nativeQuery=true)*/