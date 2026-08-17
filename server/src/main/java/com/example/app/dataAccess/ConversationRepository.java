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

    /** Direct chats only. Build the key with ChatService.directKeyFor so the order is normalised. */
    Optional<Conversation> findByDirectKey(String directKey);

    /**
     * Takes the row's write lock so the caller can safely increment lastSeq. Only the people in
     * this conversation ever contend for it, so the lock is effectively free.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conversation c WHERE c.id = :id")
    Optional<Conversation> findByIdForUpdate(@Param("id") Long id);

    /**
     * The chat list. Join-fetches every participant and their user in one go: each row renders
     * either the other person's name (direct) or the member list (group), and fetching that per
     * conversation would be an N+1 that grows with the number of chats.
     *
     * The subquery picks the conversations; the fetch join then pulls ALL participants of those,
     * not just the viewer's own row. Filtering in the join instead would silently return
     * conversations containing one member.
     */
    @Query("""
    SELECT DISTINCT c FROM Conversation c
    JOIN FETCH c.participants p
    JOIN FETCH p.user
    WHERE c.id IN (
        SELECT cp.conversation.id FROM ConversationParticipant cp WHERE cp.user.id = :userId
    )
    ORDER BY c.lastMessageAt DESC
    """)
    List<Conversation> findByParticipant(@Param("userId") Long userId);

    /** One conversation with its participants loaded -- for the responses that render members. */
    @Query("""
    SELECT DISTINCT c FROM Conversation c
    JOIN FETCH c.participants p
    JOIN FETCH p.user
    WHERE c.id = :id
    """)
    Optional<Conversation> findByIdWithParticipants(@Param("id") Long id);
}
