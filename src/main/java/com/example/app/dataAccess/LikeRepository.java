package com.example.app.dataAccess;

import com.example.app.dto.UserLikeProjection;
import com.example.app.entities.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LikeRepository extends JpaRepository<Like,Long> {
    List<Like> findByUserIdAndPostId(Long userId, Long postId);

    List<Like> findByUserId(Long userId);

    List<Like> findByPostId(Long postId);

    @Query(value = """
    SELECT l.post_id AS postId, u.image AS image, u.username AS username
    FROM post_like l
    LEFT JOIN users u ON u.id = l.user_id
    WHERE l.post_id IN :postIds
    LIMIT 5
    """, nativeQuery = true)
    List<UserLikeProjection> findUserLikesByPostId(@Param("postIds") List<Long> postIds);
}
