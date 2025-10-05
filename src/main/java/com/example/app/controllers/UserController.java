package com.example.app.controllers;

import com.example.app.entities.User;
import com.example.app.exceptions.NotFoundException;
import com.example.app.requests.UserRequest;
import com.example.app.responses.UserResponse;
import com.example.app.security.JWTUserDetails;
import com.example.app.services.UserService;
import com.example.app.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private final UserService userService;

    public UserController(UserService userService){  //constructor injection
        this.userService=userService;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(){
        var a = userService.getAllUsers();
        var list =  a.stream()
                .map(UserResponse::new)   // calls new UserResponse(user) for each
                .toList();
        return ApiResponse.success(list);
    }
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal JWTUserDetails usr){
        User user = userService.getUserByIdOrThrow(usr.getId());
        return ApiResponse.success(new UserResponse(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@AuthenticationPrincipal JWTUserDetails user, @RequestBody UserRequest newUser){
        User updatedUser = userService.updateUserById(user.getId(), newUser);
        if(updatedUser==null){
            throw new NotFoundException();
        }
        return ApiResponse.success(new UserResponse(updatedUser));
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteCurrentUser(@AuthenticationPrincipal JWTUserDetails user){
        userService.deleteUserById(user.getId());
        return ApiResponse.build(HttpStatus.NO_CONTENT, "success", null);
    }

    @GetMapping("/activity/me")
    public ResponseEntity<?> getCurrentUserActivity(@AuthenticationPrincipal JWTUserDetails user){
        var res = userService.getUserActivityById(user.getId());
        if(res==null){
            throw new IllegalArgumentException();
        }
        return ApiResponse.success(res);
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
    public ResponseEntity<?> getUserActivityById(@PathVariable Long userId){
        var res = userService.getUserActivityById(userId);
        if(res==null){
            throw new IllegalArgumentException();
        }
        return ApiResponse.success(res);
    }
}
