package com.example.app.services;

import com.example.app.dataAccess.FriendRequestRepository;
import com.example.app.entities.FriendRequest;
import com.example.app.entities.FriendRequestStatus;
import com.example.app.entities.User;
import com.example.app.exceptions.ConflictException;
import com.example.app.exceptions.ForbiddenException;
import com.example.app.exceptions.NotFoundException;
import com.example.app.responses.FriendRequestResponse;
import com.example.app.responses.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final UserService userService;

    public FriendRequestService(FriendRequestRepository friendRequestRepository,
                                UserService userService) {
        this.friendRequestRepository = friendRequestRepository;
        this.userService = userService;
    }

    /**
     * Sends a friend request, or accepts the other user's if they already sent one -- asking back is
     * unambiguously agreement, and it avoids leaving two PENDING rows for one relationship.
     *
     * @throws IllegalArgumentException if addresseeId is missing or is the caller's own id
     * @throws NotFoundException       if addresseeId has no row
     * @throws ConflictException       if a request is already pending or the two are already friends
     */
    @Transactional
    public FriendRequestResponse send(Long requesterId, Long addresseeId) {
        if (addresseeId == null) {
            throw new IllegalArgumentException("addresseeId is required");
        }
        if (requesterId.equals(addresseeId)) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself");
        }

        User addressee = userService.getUserByIdOrThrow(addresseeId);
        User requester = userService.getUserByIdOrThrow(requesterId);

        Optional<FriendRequest> theirs =
                friendRequestRepository.findByRequesterIdAndAddresseeId(addresseeId, requesterId);
        if (theirs.isPresent()) {
            FriendRequest reverse = theirs.get();
            switch (reverse.getStatus()) {
                case PENDING -> {
                    return new FriendRequestResponse(respond(reverse, FriendRequestStatus.ACCEPTED));
                }
                case ACCEPTED -> throw new ConflictException("You are already friends");
                // They asked, this user declined. Nothing stops them asking the other way now.
                case DECLINED -> { }
            }
        }

        Optional<FriendRequest> mine =
                friendRequestRepository.findByRequesterIdAndAddresseeId(requesterId, addresseeId);
        if (mine.isPresent()) {
            FriendRequest existing = mine.get();
            switch (existing.getStatus()) {
                case PENDING -> throw new ConflictException("Friend request already sent");
                case ACCEPTED -> throw new ConflictException("You are already friends");
                // Declined before: reuse the row rather than insert, which the unique pair forbids.
                case DECLINED -> {
                    existing.setStatus(FriendRequestStatus.PENDING);
                    existing.setCreateDate(LocalDateTime.now());
                    existing.setRespondDate(null);
                    return new FriendRequestResponse(friendRequestRepository.save(existing));
                }
            }
        }

        FriendRequest request = new FriendRequest();
        request.setRequester(requester);
        request.setAddressee(addressee);
        request.setStatus(FriendRequestStatus.PENDING);
        request.setCreateDate(LocalDateTime.now());
        return new FriendRequestResponse(friendRequestRepository.save(request));
    }

    /** Requests waiting on this user -- the "people who sent me a friend request" list. */
    public List<FriendRequestResponse> listIncoming(Long userId) {
        return toResponses(friendRequestRepository.findByAddressee(userId, FriendRequestStatus.PENDING));
    }

    /** Everything this user sent, accepted or not -- the "requests I sent" list. */
    public List<FriendRequestResponse> listOutgoing(Long userId) {
        return toResponses(friendRequestRepository.findByRequester(userId));
    }

    /** The other party from every accepted row, whichever side of it this user is on. */
    public List<UserResponse> listFriends(Long userId) {
        return friendRequestRepository.findInvolvingUser(userId, FriendRequestStatus.ACCEPTED).stream()
                .map(request -> request.getRequester().getId().equals(userId)
                        ? request.getAddressee()
                        : request.getRequester())
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * @throws ForbiddenException if the caller is not the addressee -- only the person asked decides
     * @throws ConflictException  if the request has already been answered
     */
    @Transactional
    public FriendRequestResponse accept(Long requestId, Long userId) {
        return new FriendRequestResponse(
                respond(requireAddressee(requestId, userId), FriendRequestStatus.ACCEPTED));
    }

    /** @see #accept(Long, Long) for the guards */
    @Transactional
    public FriendRequestResponse decline(Long requestId, Long userId) {
        return new FriendRequestResponse(
                respond(requireAddressee(requestId, userId), FriendRequestStatus.DECLINED));
    }

    /**
     * Cancels a pending request the caller sent, or unfriends someone. Both are the same row being
     * removed, so declining is deliberately not routed here -- that keeps the row as history.
     *
     * @throws ForbiddenException if the caller is not part of this relationship, or is trying to
     *                            cancel a pending request that someone else sent them
     */
    @Transactional
    public void delete(Long requestId, Long userId) {
        FriendRequest request = getByIdOrThrow(requestId);
        boolean isRequester = request.getRequester().getId().equals(userId);
        boolean isAddressee = request.getAddressee().getId().equals(userId);

        if (!isRequester && !isAddressee) {
            throw new ForbiddenException("Not your friend request");
        }
        if (request.getStatus() == FriendRequestStatus.PENDING && !isRequester) {
            throw new ForbiddenException("Only the sender can cancel a pending request -- decline it instead");
        }
        friendRequestRepository.delete(request);
    }

    /**
     * Unfriends by the other person's id rather than the row's. A friends list renders users, not
     * requests, so it has no request id to pass to delete() -- and which of the two sent the
     * original request is not something the client should have to know.
     *
     * @throws NotFoundException if the two are not currently friends
     */
    @Transactional
    public void removeFriend(Long userId, Long otherUserId) {
        FriendRequest friendship = findAccepted(userId, otherUserId)
                .or(() -> findAccepted(otherUserId, userId))
                .orElseThrow(() -> new NotFoundException("You are not friends with this user"));
        friendRequestRepository.delete(friendship);
    }

    /**
     * Whether these two are friends, checked in both directions because the row records who asked.
     * Two indexed existence checks rather than one OR query, so neither side needs a scan.
     */
    public boolean areFriends(Long userId, Long otherUserId) {
        return friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(
                        userId, otherUserId, FriendRequestStatus.ACCEPTED)
                || friendRequestRepository.existsByRequesterIdAndAddresseeIdAndStatus(
                        otherUserId, userId, FriendRequestStatus.ACCEPTED);
    }

    public FriendRequest getByIdOrThrow(Long requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Friend request not found"));
    }

    /** Empty unless a row exists in exactly this direction and it is ACCEPTED. */
    private Optional<FriendRequest> findAccepted(Long requesterId, Long addresseeId) {
        return friendRequestRepository.findByRequesterIdAndAddresseeId(requesterId, addresseeId)
                .filter(request -> request.getStatus() == FriendRequestStatus.ACCEPTED);
    }

    private FriendRequest requireAddressee(Long requestId, Long userId) {
        FriendRequest request = getByIdOrThrow(requestId);
        if (!request.getAddressee().getId().equals(userId)) {
            throw new ForbiddenException("Only the recipient can answer this friend request");
        }
        return request;
    }

    private FriendRequest respond(FriendRequest request, FriendRequestStatus status) {
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ConflictException("This friend request was already "
                    + request.getStatus().name().toLowerCase());
        }
        request.setStatus(status);
        request.setRespondDate(LocalDateTime.now());
        return friendRequestRepository.save(request);
    }

    private static List<FriendRequestResponse> toResponses(List<FriendRequest> requests) {
        return requests.stream().map(FriendRequestResponse::new).collect(Collectors.toList());
    }
}
