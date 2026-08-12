package com.example.app.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * One message in a conversation. Ordered by seq, never by createDate -- timestamps tie and clocks
 * move, and a unique (conversation_id, seq) makes the ordering total and gap-free.
 */
@Entity
@Data
@Table(name = "message", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"conversation_id", "seq"})
})
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User sender;

    /** Position within this conversation, starting at 1. */
    @Column(nullable = false)
    private Long seq;

    @Column(columnDefinition = "text", nullable = false)
    private String body;

    private LocalDateTime createDate;
}
