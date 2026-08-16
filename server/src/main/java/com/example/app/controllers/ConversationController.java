package com.example.app.controllers;

import com.example.app.requests.CreateConversationRequest;
import com.example.app.requests.CreateMessageRequest;
import com.example.app.requests.MarkConversationReadRequest;
import com.example.app.responses.ConversationResponse;
import com.example.app.responses.MessageResponse;
import com.example.app.security.JWTUserDetails;
import com.example.app.services.ChatService;
import com.example.app.utils.Response;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
public class ConversationController {
    private final ChatService chatService;

    public ConversationController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public Response<List<ConversationResponse>> list(@AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(chatService.listConversations(user.getId()));
    }

    /**
     * Opens the chat with a friend, creating it if this is the first message either way. Returns 200
     * rather than 201 because it is idempotent -- the caller cannot tell, and should not care,
     * whether the row already existed.
     */
    @PostMapping
    public Response<ConversationResponse> open(@RequestBody CreateConversationRequest request,
                                               @AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(chatService.getOrCreate(user.getId(), request.getUserId()));
    }

    /** The whole conversation, oldest first. */
    @GetMapping("/{id}/messages")
    public Response<List<MessageResponse>> messages(@PathVariable Long id,
                                                    @AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(chatService.listMessages(id, user.getId()));
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public Response<MessageResponse> send(@PathVariable Long id,
                                          @RequestBody CreateMessageRequest request,
                                          @AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(chatService.sendMessage(id, user.getId(), request.getBody()));
    }

    /**
     * Clears the caller's unread badge up to the message they have seen. Returns the conversation
     * so the client can settle its own cache without re-listing. Idempotent, and the read pointer
     * never moves backwards, so this is safe to send on every poll.
     */
    @PostMapping("/{id}/read")
    public Response<ConversationResponse> markRead(@PathVariable Long id,
                                                   @RequestBody MarkConversationReadRequest request,
                                                   @AuthenticationPrincipal JWTUserDetails user) {
        return Response.success(chatService.markRead(id, user.getId(), request.getLastReadSeq()));
    }
}
