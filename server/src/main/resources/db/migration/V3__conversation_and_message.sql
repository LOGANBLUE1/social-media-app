-- 1:1 chat.
--
-- The user pair is normalised into (low, high) with a CHECK enforcing the order, so a conversation
-- has exactly one row per pair and is found by a single indexed lookup rather than an OR. That
-- works here, unlike friend_request, because a conversation is symmetric -- nobody "sent" it.
--
-- Messages are ordered by `seq`, a per-conversation counter, not by create_date: timestamps tie and
-- clocks move. `conversation.last_seq` holds the counter, incremented under that row's lock so two
-- senders cannot claim the same seq.
--
-- NOTE: Flyway is not on the classpath yet, so this file is not executed automatically.

CREATE TABLE IF NOT EXISTS conversation (
    id              BIGSERIAL PRIMARY KEY,
    user_low_id     BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_high_id    BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_seq        BIGINT    NOT NULL DEFAULT 0,
    last_message_at TIMESTAMP,
    create_date     TIMESTAMP,
    CONSTRAINT uq_conversation_pair     UNIQUE (user_low_id, user_high_id),
    CONSTRAINT ck_conversation_ordered  CHECK (user_low_id < user_high_id)
);

CREATE INDEX IF NOT EXISTS idx_conversation_user_low  ON conversation (user_low_id, last_message_at DESC);
CREATE INDEX IF NOT EXISTS idx_conversation_user_high ON conversation (user_high_id, last_message_at DESC);

CREATE TABLE IF NOT EXISTS message (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT    NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    sender_id       BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    seq             BIGINT    NOT NULL,
    body            TEXT      NOT NULL,
    create_date     TIMESTAMP,
    CONSTRAINT uq_message_conversation_seq UNIQUE (conversation_id, seq)
);

-- Serves both "load the whole chat in order" and any later paging on seq.
CREATE INDEX IF NOT EXISTS idx_message_conversation_seq ON message (conversation_id, seq);
