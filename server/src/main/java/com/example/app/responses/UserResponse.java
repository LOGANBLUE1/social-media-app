package com.example.app.responses;


import com.example.app.entities.User;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String image;

    public UserResponse(User user) {
        this.id = user.getId();
        this.image = user.getImage();
        this.username = user.getUsername();
    }
}
