package com.example.app.responses;

import com.example.app.entities.FriendRequest;
import com.example.app.entities.FriendRequestStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Both users are included rather than just "the other one", because the incoming and outgoing lists
 * render opposite sides of the same row and the client should not have to infer which is which.
 */
@Data
public class FriendRequestResponse {
    private Long id;
    private UserResponse requester;
    private UserResponse addressee;
    private FriendRequestStatus status;
    private LocalDateTime createdAt;
    /** Null while the request is still PENDING. */
    private LocalDateTime respondedAt;

    public FriendRequestResponse(FriendRequest request) {
        this.id = request.getId();
        this.requester = new UserResponse(request.getRequester());
        this.addressee = new UserResponse(request.getAddressee());
        this.status = request.getStatus();
        this.createdAt = request.getCreateDate();
        this.respondedAt = request.getRespondDate();
    }
}