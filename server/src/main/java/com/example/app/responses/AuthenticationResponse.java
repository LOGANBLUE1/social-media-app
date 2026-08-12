package com.example.app.responses;

import com.example.app.entities.User;
import lombok.Data;

@Data
public class AuthenticationResponse {
    String accessToken;
    String refreshToken;
    UserResponse user;
}

