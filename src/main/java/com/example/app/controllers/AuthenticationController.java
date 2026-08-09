package com.example.app.controllers;

import com.example.app.requests.ChangePasswordRequest;
import com.example.app.requests.RefreshTokenRequest;
import com.example.app.requests.UserRequest;
import com.example.app.responses.AuthenticationResponse;
import com.example.app.security.JWTUserDetails;
import com.example.app.services.AuthService;
import com.example.app.utils.Response;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final AuthService authService;

    public AuthenticationController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Response<AuthenticationResponse> login(@RequestBody UserRequest request) {
        return Response.success(authService.login(request), "Login successful");
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Response<AuthenticationResponse> register(@RequestBody UserRequest request) {
        return Response.success(authService.register(request), "User Successfully Registered");
    }

    @PostMapping("/refresh")
    public Response<AuthenticationResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return Response.success(authService.refresh(request), "token successfully refreshed.");
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal JWTUserDetails user,
                               @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
    }
}
