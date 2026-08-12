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

    private LocalDateTime createDate;
}
