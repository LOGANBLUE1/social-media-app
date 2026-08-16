package com.example.app.dataAccess;

import com.example.app.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Looked up by the token itself, not by user id: a user now holds one row per signed-in device,
     * so the id no longer identifies a single row. Deriving the user from the row is also stricter
     * than trusting a caller-supplied id -- it makes presenting someone else's token unhelpful.
     */
    RefreshToken findByToken(String token);

    /** Housekeeping, so signing in repeatedly does not leave dead rows behind forever. */
    void deleteByUserIdAndExpiryDateBefore(Long userId, LocalDateTime cutoff);
}
