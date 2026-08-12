package com.example.app.dataAccess;

import com.example.app.dto.PostCountProjection;
import com.example.app.dto.UserLikeProjection;
import com.example.app.entities.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like,Long> {
    List<Like> findByUserIdAndPostId(Long userId, Long postId);

    List<Like> findByUserId(Long userId);

    List<Like> findByPostId(Long postId);

    /** At most one row: (post_id, user_id) is unique on post_like. */
    Optional<Like> findFirstByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    @Query(value = """
    SELECT post_id AS postId, COUNT(*) AS total
    FROM post_like
    WHERE post_id IN :postIds
    GROUP BY post_id
    """, nativeQuery = true)
    List<PostCountProjection> countByPostIds(@Param("postIds") List<Long> postIds);

    /** Which of these posts the given user has liked -- one query for a whole page of posts. */
    @Query(value = """
    SELECT post_id
    FROM post_like
    WHERE user_id = :userId AND post_id IN :postIds
    """, nativeQuery = true)
    List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);

    @Query(value = """
    SELECT l.post_id AS postId, u.image AS image, u.username AS username
    FROM post_like l
    LEFT JOIN users u ON u.id = l.user_id
    WHERE l.post_id IN :postIds
    LIMIT 5
    """, nativeQuery = true)
    List<UserLikeProjection> findUserLikesByPostId(@Param("postIds") List<Long> postIds);
}
