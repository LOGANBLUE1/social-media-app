package com.example.app.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * One row per relationship between two users, in both its requested and its accepted state: a
 * PENDING row is a friend request, an ACCEPTED row is the friendship itself. Keeping them in one
 * table means accepting is a status change rather than a move between tables.
 *
 * The row is directional -- requester asked addressee -- even though friendship is symmetric, so
 * queries for "my friends" must check both columns. The unique constraint prevents the same user
 * asking twice, but cannot express "unique unordered pair"; the reverse-direction request is
 * handled in FriendRequestService instead.
 */
@Entity
@Data
@Table(name = "friend_request", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"requester_id", "addressee_id"})
})
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who clicked "Add friend". */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)  // If the user is deleted, also delete their requests.
    private User requester;

    /** Who has to decide. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FriendRequestStatus status;

    private LocalDateTime createDate;

    /** When the addressee accepted or declined; null while still PENDING. */
    private LocalDateTime respondDate;
}