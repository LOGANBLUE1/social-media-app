package com.example.app.controllers;

import com.example.app.entities.User;
import com.example.app.exceptions.NotFoundException;
import com.example.app.requests.UserRequest;
import com.example.app.responses.UserActivityResponse;
import com.example.app.responses.UserResponse;
import com.example.app.security.JWTUserDetails;
import com.example.app.services.UserService;
import com.example.app.utils.Response;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){  //constructor injection
        this.userService=userService;
    }

    @GetMapping
    public Response<List<UserResponse>> getAllUsers(){
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserResponse::new)   // calls new UserResponse(user) for each
                .toList();
        return Response.success(users);
    }

    @GetMapping("/me")
    public Response<UserResponse> getCurrentUser(@AuthenticationPrincipal JWTUserDetails usr){
        User user = userService.getUserByIdOrThrow(usr.getId());
        return Response.success(new UserResponse(user));
    }

    @PutMapping("/me")
    public Response<UserResponse> updateCurrentUser(@AuthenticationPrincipal JWTUserDetails user, @RequestBody UserRequest newUser){
        User updatedUser = userService.updateUserById(user.getId(), newUser);
        if(updatedUser==null){
            throw new NotFoundException();
        }
        return Response.success(new UserResponse(updatedUser));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrentUser(@AuthenticationPrincipal JWTUserDetails user){
        userService.deleteUserById(user.getId());
    }

    @GetMapping("/activity/me")
    public Response<UserActivityResponse> getCurrentUserActivity(@AuthenticationPrincipal JWTUserDetails user){
        return Response.success(activityOrThrow(user.getId()));
    }

//    @PostMapping
//    public User createUser(@RequestBody User user){
//        return userService.createUser(user);
//    }
//

//    @PutMapping("/{userId}")   //update
//    public User updateUserById(@PathVariable Long userId, @RequestBody User newUser){
//        return userService.updateUserById(userId, newUser);
//    }
//
//    @DeleteMapping("/{userId}") //delete
//    public void deleteUserById(@PathVariable Long userId){
//        userService.deleteUserById(userId);
//    }
//    @GetMapping("/{userId}")
//    public UserResponse getUserById(@PathVariable Long userId){
//        User user1 = userService.getUserById(userId);
//        if(user1==null){
//            throw new UserNotFoundException();
//        }
//        return new UserResponse(user1);
//    }

    @GetMapping("/activity/{userId}")
    public Response<UserActivityResponse> getUserActivityById(@PathVariable Long userId){
        return Response.success(activityOrThrow(userId));
    }

    private UserActivityResponse activityOrThrow(Long userId) {
        UserActivityResponse activity = userService.getUserActivityById(userId);
        if (activity == null) {
            throw new IllegalArgumentException("No activity found for this user");
        }
        return activity;
    }
}