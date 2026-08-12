package com.example.app.controllers;

import com.example.app.requests.CreateFriendRequest;
import com.example.app.responses.FriendRequestResponse;
import com.example.app.responses.UserResponse;
import com.example.app.security.JWTUserDetails;
import com.example.app.services.FriendRequestService;
import com.example.app.utils.Response;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friends")
public class FriendRequestController {
    private final FriendRequestService friendRequestService;

    public FriendRequestController(FriendRequestService friendRequestService) {
        this.friendRequestService = friendRequestService;
    }

    /** The "Add friend" button. Accepts instead of sending if the other user already asked. */
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Response<FriendRequestResponse> sendRequest(@RequestBody CreateFriendRequest request,
                                                       @AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(friendRequestService.send(user.getId(), request.getAddresseeId()));
    }

    @GetMapping("/requests/incoming")
    public Response<List<FriendRequestResponse>> incoming(@AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(friendRequestService.listIncoming(user.getId()));
    }

    @GetMapping("/requests/outgoing")
    public Response<List<FriendRequestResponse>> outgoing(@AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(friendRequestService.listOutgoing(user.getId()));
    }

    @PostMapping("/requests/{id}/accept")
    public Response<FriendRequestResponse> accept(@PathVariable Long id,
                                                  @AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(friendRequestService.accept(id, user.getId()));
    }

    @PostMapping("/requests/{id}/decline")
    public Response<FriendRequestResponse> decline(@PathVariable Long id,
                                                   @AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(friendRequestService.decline(id, user.getId()));
    }

    /** Cancels a request this user sent, or unfriends. Declining is POST /decline, not this. */
    @DeleteMapping("/requests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal JWTUserDetails user) {
        friendRequestService.delete(id, user.getId());
    }

    @GetMapping
    public Response<List<UserResponse>> friends(@AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(friendRequestService.listFriends(user.getId()));
    }

    /**
     * Unfriend, addressed by the other user rather than the underlying request row -- what a friends
     * list can actually call. Equivalent to DELETE /friends/requests/{id} on an accepted row.
     */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(@PathVariable Long userId,
                             @AuthenticationPrincipal JWTUserDetails user) {
        friendRequestService.removeFriend(user.getId(), userId);
    }
}
