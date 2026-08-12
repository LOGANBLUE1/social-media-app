package com.example.app.dataAccess;

import com.example.app.entities.Conversation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** Ids must be passed in normalised order -- see ChatService.getOrCreate. */
    Optional<Conversation> findByUserLowIdAndUserHighId(Long userLowId, Long userHighId);

    /**
     * Takes the row's write lock so the caller can safely increment lastSeq. Only the two people in
     * this conversation ever contend for it, so the lock is effectively free.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conversation c WHERE c.id = :id")
    Optional<Conversation> findByIdForUpdate(@Param("id") Long id);

    /** The chat list. Join-fetches both sides because every row renders the other user. */
    @Query("""
    SELECT c FROM Conversation c
    JOIN FETCH c.userLow
    JOIN FETCH c.userHigh
    WHERE c.userLow.id = :userId OR c.userHigh.id = :userId
    ORDER BY c.lastMessageAt DESC
    """)
    List<Conversation> findByParticipant(@Param("userId") Long userId);
}
