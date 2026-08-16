package com.example.app.services;

import com.example.app.dataAccess.RefreshTokenRepository;
import com.example.app.entities.RefreshToken;
import com.example.app.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${refresh.token.expires.in}")
    Long expireSeconds;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken getByToken(String token) {
        return token == null ? null : refreshTokenRepository.findByToken(token);
    }

    /**
     * Issues a refresh token for one sign-in.
     *
     * This used to overwrite the user's single row, which made signing in anywhere a silent sign-out
     * everywhere else -- log in on the web and the phone's next refresh would fail, with no way to
     * tell that from an expired session. A row per sign-in means a device keeps working until its
     * own token lapses.
     */
    @Transactional
    public String createRefreshToken(User user) {
        // Sweep this user's dead rows on the way through, so the table does not grow forever
        // without needing a scheduled job.
        refreshTokenRepository.deleteByUserIdAndExpiryDateBefore(user.getId(), LocalDateTime.now());

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(LocalDateTime.now().plusSeconds(expireSeconds));
        refreshTokenRepository.save(token);
        return token.getToken();
    }

    /**
     * Pushes the expiry back out on use, making the window sliding rather than fixed. Without this
     * a session died a set time after *login* however actively it was being used, which for a
     * daily-driver app means a forced re-login on a schedule for no reason.
     */
    @Transactional
    public void extend(RefreshToken token) {
        token.setExpiryDate(LocalDateTime.now().plusSeconds(expireSeconds));
        refreshTokenRepository.save(token);
    }

    public boolean isRefreshExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(LocalDateTime.now());
    }

}