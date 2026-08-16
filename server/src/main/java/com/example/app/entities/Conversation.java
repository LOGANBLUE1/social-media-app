package com.example.app.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * A 1:1 chat between two users.
 *
 * The pair is stored normalised -- userLow always has the smaller id -- so there is exactly one row
 * per pair and lookups need no OR over both columns. A conversation is symmetric, so unlike
 * FriendRequest there is no meaning to lose by discarding the direction.
 */
@Entity
@Data
@Table(name = "conversation", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_low_id", "user_high_id"})
})
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_low_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User userLow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_high_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User userHigh;

    /**
     * The per-conversation message counter. Incremented while holding this row's write lock, which
     * is what stops two simultaneous senders claiming the same seq.
     */
    @Column(nullable = false)
    private Long lastSeq = 0L;

    /** Denormalised so the chat list sorts without touching the message table. */
    private LocalDateTime lastMessageAt;

    /**
     * How far each participant has read, as a seq. Two columns rather than a participant table,
     * for the same reason the pair itself is stored low/high: a 1:1 chat has exactly two sides.
     *
     * Kept in step with {@link #lastSeq} by ChatService, which advances the *sender's* pointer on
     * every send. That invariant is what lets the unread count be plain subtraction -- see
     * {@link #unreadCountFor}.
     */
    @Column(nullable = false)
    private Long userLowLastReadSeq = 0L;

    @Column(nullable = false)
    private Long userHighLastReadSeq = 0L;

    private LocalDateTime createDate;

    /** The given participant's read position. Assumes they are one -- callers check first. */
    public Long lastReadSeqFor(Long userId) {
        return userLow.getId().equals(userId) ? userLowLastReadSeq : userHighLastReadSeq;
    }

    public void applyLastReadSeqFor(Long userId, Long seq) {
        if (userLow.getId().equals(userId)) {
            userLowLastReadSeq = seq;
        } else {
            userHighLastReadSeq = seq;
        }
    }

    /**
     * Unread messages for one participant: everything written since they last read. No COUNT --
     * seq is gap-free, and a sender's own pointer moves with their message, so nothing above the
     * pointer can be the viewer's own.
     */
    public long unreadCountFor(Long userId) {
        return Math.max(0L, lastSeq - lastReadSeqFor(userId));
    }
}
