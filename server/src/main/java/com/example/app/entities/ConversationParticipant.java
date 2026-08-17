package com.example.app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * One person's membership of one conversation, and how far they have read it.
 *
 * This is the single home for both facts. They used to be spread across the conversation row --
 * membership in the user_low/user_high pair, read position in two columns beside it -- which only
 * worked while every room had exactly two people in it.
 *
 * Lombok's @Data is deliberately avoided here: it generates equals/hashCode over every field,
 * including the conversation back-reference, and that recurses through the conversation's
 * participant collection.
 */
@Entity
@Getter
@Setter
@Table(name = "conversation_participant", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"conversation_id", "user_id"})
})
public class ConversationParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /**
     * The newest seq this person has seen. Unread is lastSeq minus this -- see
     * {@link Conversation#unreadCountFor}.
     */
    @Column(nullable = false)
    private Long lastReadSeq = 0L;

    private LocalDateTime joinDate;
}
