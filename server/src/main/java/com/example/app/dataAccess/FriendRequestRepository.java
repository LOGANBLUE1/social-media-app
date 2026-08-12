package com.example.app.dataAccess;

import com.example.app.entities.FriendRequest;
import com.example.app.entities.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * The list queries join-fetch both users because every one of them renders usernames -- leaving the
 * LAZY associations to resolve themselves would be a query per row.
 */
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    /** At most one row per direction: (requester_id, addressee_id) is unique. */
    Optional<FriendRequest> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    boolean existsByRequesterIdAndAddresseeIdAndStatus(Long requesterId, Long addresseeId,
                                                       FriendRequestStatus status);

    /** Requests waiting on this user's decision. */
    @Query("""
    SELECT fr FROM FriendRequest fr
    JOIN FETCH fr.requester
    JOIN FETCH fr.addressee
    WHERE fr.addressee.id = :userId AND fr.status = :status
    ORDER BY fr.createDate DESC
    """)
    List<FriendRequest> findByAddressee(@Param("userId") Long userId,
                                        @Param("status") FriendRequestStatus status);

    /** Everything this user has sent, whatever became of it. */
    @Query("""
    SELECT fr FROM FriendRequest fr
    JOIN FETCH fr.requester
    JOIN FETCH fr.addressee
    WHERE fr.requester.id = :userId
    ORDER BY fr.createDate DESC
    """)
    List<FriendRequest> findByRequester(@Param("userId") Long userId);

    /** Rows in the given status where this user is on either side -- how a friends list is read. */
    @Query("""
    SELECT fr FROM FriendRequest fr
    JOIN FETCH fr.requester
    JOIN FETCH fr.addressee
    WHERE fr.status = :status
      AND (fr.requester.id = :userId OR fr.addressee.id = :userId)
    ORDER BY fr.respondDate DESC
    """)
    List<FriendRequest> findInvolvingUser(@Param("userId") Long userId,
                                          @Param("status") FriendRequestStatus status);
}