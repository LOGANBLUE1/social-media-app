package com.example.app.services;

import com.example.app.entities.RefreshToken;
import com.example.app.entities.User;
import com.example.app.exceptions.ConflictException;
import com.example.app.exceptions.NotFoundException;
import com.example.app.exceptions.UnauthorizedException;
import com.example.app.requests.ChangePasswordRequest;
import com.example.app.requests.RefreshTokenRequest;
import com.example.app.requests.UserRequest;
import com.example.app.responses.AuthenticationResponse;
import com.example.app.responses.UserResponse;
import com.example.app.security.JWTTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns everything credential-related: verifying passwords, encoding them before they are
 * stored, and issuing access/refresh tokens. UserService handles profile fields only and
 * never touches User.password, which keeps the encoder to a single owner.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JWTTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JWTTokenProvider jwtTokenProvider,
                       RefreshTokenService refreshTokenService,
                       UserService userService,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * @throws AuthenticationException if the username/password pair is rejected. Raised inside
     *                                 authenticationManager.authenticate, not by this class.
     */
    public AuthenticationResponse login(UserRequest request) {
        String accessToken = authenticate(request);
        User user = userService.getUserByUsername(request.getUsername());
        return authenticationResponse(user, accessToken);
    }

    /**
     * @throws ConflictException       if the username is taken
     * @throws AuthenticationException if the just-saved credentials fail to authenticate. Raised
     *                                 inside authenticationManager.authenticate, not by this class.
     */
    public AuthenticationResponse register(UserRequest request) {
        if (userService.getUserByUsername(request.getUsername()) != null) {
            throw new ConflictException("Username is already taken");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setImage(request.getImage());
        userService.save(user);

        return authenticationResponse(user, authenticate(request));
    }

    /**
     * @throws NotFoundException     if userId has no row. Raised by userService.getUserByIdOrThrow.
     * @throws UnauthorizedException if currentPassword does not match the stored hash
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("newPassword is required");
        }
        User user = userService.getUserByIdOrThrow(userId);
        // proving knowledge of the current password is what stops a stolen token
        // from being escalated into a permanent account takeover
        if (request.getCurrentPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.save(user);
    }

    /**
     * Trades a refresh token for a fresh access token, and pushes the refresh token's own expiry
     * back out -- so a user who keeps using the app is never signed out on a timer.
     *
     * The lookup is by token value alone. request.getUserId() is ignored: the row already knows
     * whose it is, and taking the owner from the row rather than the request means a caller cannot
     * name someone else. The field stays on the request for wire compatibility with the client.
     */
    @Transactional
    public AuthenticationResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenService.getByToken(request.getRefreshToken());
        if (token == null || refreshTokenService.isRefreshExpired(token)) {
            throw new UnauthorizedException("refresh token is not valid.");
        }
        refreshTokenService.extend(token);

        AuthenticationResponse body = new AuthenticationResponse();
        body.setAccessToken(jwtTokenProvider.generateJwtTokenByUserId(token.getUser().getId()));
        return body;
    }

    // Verifies the credentials and mints a fresh access token. Throws AuthenticationException
    // on a bad username/password, which GlobalExceptionHandler renders as a 401.
    private String authenticate(UserRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        return jwtTokenProvider.generateJWTToken(auth);
    }

    private AuthenticationResponse authenticationResponse(User user, String accessToken) {
        AuthenticationResponse body = new AuthenticationResponse();
        body.setAccessToken(accessToken);
        body.setRefreshToken(refreshTokenService.createRefreshToken(user));
        body.setUser(new UserResponse(user));
        return body;
    }
}
