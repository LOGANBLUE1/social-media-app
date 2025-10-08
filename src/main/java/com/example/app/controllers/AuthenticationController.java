package com.example.app.controllers;

import com.example.app.entities.RefreshToken;
import com.example.app.entities.User;
import com.example.app.exceptions.NotFoundException;
import com.example.app.requests.RefreshTokenRequest;
import com.example.app.requests.UserRequest;
import com.example.app.responses.AuthenticationResponse;
import com.example.app.responses.UserResponse;
import com.example.app.security.JWTTokenProvider;
import com.example.app.services.RefreshTokenService;
import com.example.app.services.UserService;
import com.example.app.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final JWTTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationController(JWTTokenProvider jwtTokenProvider,
                                    AuthenticationManager authenticationManager,
                                    PasswordEncoder passwordEncoder,
                                    UserService userService,
                                    RefreshTokenService refreshTokenService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserRequest request) {
        try {
            // Attempt authentication
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
            Authentication auth = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Fetch user and generate JWT
            User user = userService.getUserByUsername(request.getUsername());
            String jwtToken = jwtTokenProvider.generateJWTToken(auth);

            AuthenticationResponse body = new AuthenticationResponse();
            body.setAccessToken(jwtToken);
            body.setRefreshToken(refreshTokenService.createRefreshToken(user));
            body.setUser(new UserResponse(user));

            return ApiResponse.build(HttpStatus.OK, "Login successful", body);
        } catch (AuthenticationException | NotFoundException e) {
            return ApiResponse.failure(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverFailure();
        }
    }


    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody UserRequest request){
        if(userService.getUserByUsername(request.getUsername()) != null){
            return ApiResponse.failure(HttpStatus.CONFLICT, "Invalid username or password");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setImage(request.getImage());
        userService.createUser(user);

        var authToken = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        Authentication auth = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwtToken = jwtTokenProvider.generateJWTToken(auth);

        AuthenticationResponse body = new AuthenticationResponse();
        body.setAccessToken(jwtToken);
        body.setRefreshToken(refreshTokenService.createRefreshToken(user));
        body.setUser(new UserResponse(user));

        return ApiResponse.build(HttpStatus.CREATED, "User Successfully Registered", body);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        RefreshToken token = refreshTokenService.getByUser(request.getUserId());
        if(token.getToken().equals(request.getRefreshToken()) &&
                !refreshTokenService.isRefreshExpired(token)) {
            User user = token.getUser();
            String jwtToken = jwtTokenProvider.generateJwtTokenByUserId(user.getId());

            AuthenticationResponse body = new AuthenticationResponse();
            body.setAccessToken(jwtToken);
            return ApiResponse.build(HttpStatus.OK, "token successfully refreshed.", body);
        } else {
            return ApiResponse.failure(HttpStatus.UNAUTHORIZED, "refresh token is not valid.");
        }
    }

}
