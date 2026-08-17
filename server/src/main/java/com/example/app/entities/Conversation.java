package com.example.app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A room: either a 1:1 chat or a named group. The difference is the participant count and a name,
 * not the storage -- messages, seq ordering and read positions are identical for both.
 *
 * Membership lives entirely in {@link #participants}. A direct chat additionally carries
 * {@link #directKey} ("3:7", smaller id first), which is what keeps "open the chat with Bob"
 * idempotent: one indexed lookup, one row per pair. Groups leave it null, and Postgres allows
 * repeated nulls in a unique index, so any number of them coexist.
 *
 * Annotated {@code @Getter}/{@code @Setter} rather than {@code @Data}: the generated
 * equals/hashCode would walk {@code participants}, and each participant holds a reference back
 * here.
 */
@Entity
@Getter
@Setter
@Table(name = "conversation")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConversationType type = ConversationType.DIRECT;

    /** Group title. Null for direct chats, whose name is whoever you are talking to. */
    @Column(length = 120)
    private String name;

    /** Who created a group. Null for direct chats -- nobody "starts" those in a meaningful sense. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    /**
     * "<lowId>:<highId>" (smaller id first) for direct chats, null for groups. Built by
     * ChatService.directKeyFor, which is the only thing that should write it -- the normalisation
     * is what makes a pair map to one key whichever way round it is asked for.
     */
    @Column(name = "direct_key", length = 64)
    private String directKey;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConversationParticipant> participants = new ArrayList<>();

    /**
     * The per-conversation message counter. Incremented while holding this row's write lock, which
     * is what stops two simultaneous senders claiming the same seq.
     */
    @Column(nullable = false)
    private Long lastSeq = 0L;

    /** Denormalised so the chat list sorts without touching the message table. */
    private LocalDateTime lastMessageAt;

    private LocalDateTime createDate;

    public Optional<ConversationParticipant> participantFor(Long userId) {
        return participants.stream()
                .filter(participant -> participant.getUser().getId().equals(userId))
                .findFirst();
    }

    public boolean hasParticipant(Long userId) {
        return participantFor(userId).isPresent();
    }

    /** The person on the other side of a direct chat. Empty for a group, which has no "other". */
    public Optional<User> otherParticipant(Long viewerId) {
        if (type != ConversationType.DIRECT) return Optional.empty();
        return participants.stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(viewerId))
                .findFirst();
    }

    /**
     * Unread messages for one participant: everything written since they last read.
     *
     * Still subtraction rather than a COUNT, and the reasoning is unchanged by group size. seq is
     * gap-free, and ChatService advances a sender's own pointer as they send, so nothing above
     * someone's pointer was written by them. That argument was always per-participant -- the two
     * columns this replaced were an implementation detail of there being exactly two.
     */
    public long unreadCountFor(Long userId) {
        return participantFor(userId)
                .map(participant -> Math.max(0L, lastSeq - participant.getLastReadSeq()))
                .orElse(0L);
    }

    public void applyLastReadSeqFor(Long userId, Long seq) {
        participantFor(userId).ifPresent(participant -> participant.setLastReadSeq(seq));
    }

    /** Adds someone to the room at its current read position, so history does not arrive unread. */
    public void addParticipant(User user, LocalDateTime joinedAt) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversation(this);
        participant.setUser(user);
        participant.setJoinDate(joinedAt);
        // Starting at lastSeq rather than 0 means joining an existing room does not present its
        // entire backlog as unread. For a brand new room the two are the same.
        participant.setLastReadSeq(lastSeq);
        participants.add(participant);
    }
}
