package com.example.app.requests;

import lombok.Data;

/**
 * The requester is taken from the authenticated principal, never the body -- otherwise a caller
 * could send friend requests on someone else's behalf.
 */
@Data
public class CreateFriendRequest {
    private Long addresseeId;
}