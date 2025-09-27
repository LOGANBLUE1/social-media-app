package com.example.app.controllers;

import com.example.app.entities.User;
import com.example.app.exceptions.UserNotFoundException;
import com.example.app.responses.UserResponse;
import com.example.app.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private UserService userService;

    public UserController(UserService userService){  //constructor injection
        this.userService=userService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers(){
        var a = userService.getAllUsers();
        return a.stream()
                .map(UserResponse::new)   // calls new UserResponse(user) for each
                .toList();
    }

    @PostMapping
    public User createUser(@RequestBody User user){
        return userService.createUser(user);
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(@PathVariable Long userId){
        User user = userService.getUserById(userId);
        if(user==null){
            throw new UserNotFoundException();
        }
        return new UserResponse(user);
    }

    @PutMapping("/{userId}")   //update
    public User updateUserById(@PathVariable Long userId, @RequestBody User newUser){
        return userService.updateUserById(userId, newUser);
    }

    @DeleteMapping("/{userId}") //delete
    public void deleteUserById(@PathVariable Long userId){
        userService.deleteUserById(userId);
    }

    @GetMapping("/activity/{userId}")
    public List<Object> getUserActivityById(@PathVariable Long userId){
        return userService.getUserActivityById(userId);
    }

    /*@ResponseBody*/
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    private void handleUserNotFoundException(){

    }
}
