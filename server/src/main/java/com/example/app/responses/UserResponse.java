package com.example.app.responses;


import com.example.app.entities.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String image;
    /**
     * When the account was created. Null for accounts predating the column, which the client
     * reads as "not new" rather than guessing. Named to match PostResponse.createdAt.
     */
    private LocalDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.image = user.getImage();
        this.username = user.getUsername();
        this.createdAt = user.getCreateDate();
    }
}
