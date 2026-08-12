package com.example.app.dataAccess;

import com.example.app.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * The whole conversation, oldest first -- what opening a chat loads. Ordered by seq rather than
     * createDate or id, so the order is total and cannot be disturbed by clock movement.
     */
    List<Message> findByConversationIdOrderBySeqAsc(Long conversationId);
}
