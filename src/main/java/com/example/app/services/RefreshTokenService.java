package com.example.app.services;

import com.example.app.dataAccess.RefreshTokenRepository;
import com.example.app.entities.RefreshToken;
import com.example.app.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${refresh.token.expires.in}")
    Long expireSeconds;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public RefreshToken getByUser(Long userId) {
        return refreshTokenRepository.findByUserId(userId);
    }

    public String createRefreshToken(User user) {
        RefreshToken token = this.getByUser(user.getId());
        if(token == null) {
            token =	new RefreshToken();
            token.setUser(user);
        }
        token.setToken(UUID.randomUUID().toString());  //UUID ile random unique idler üretebiliriz.
        token.setExpiryDate(LocalDateTime.now().plusSeconds(expireSeconds));
        refreshTokenRepository.save(token);
        return token.getToken();
    }


    public boolean isRefreshExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(LocalDateTime.now());
    }

}