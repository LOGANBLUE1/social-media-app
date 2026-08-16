-- Per-participant read position, so the chat list can show an unread badge.
--
-- Stored as two columns on `conversation` rather than a separate participant table: the pair is
-- already normalised into (low, high) here, so the reader's pointer lives next to the writer's
-- counter and the unread count needs no join and no second query.
--
-- The count is `last_seq - <viewer's>_last_read_seq`, pure arithmetic -- no COUNT over `message`.
-- That is only sound because `seq` is gap-free (uq_message_conversation_seq, handed out under the
-- row lock) AND ChatService advances the *sender's* own pointer when they send. Together those mean
-- every message above your pointer was necessarily written by the other person.
--
-- NOTE: Flyway is not on the classpath yet, so this file is not executed automatically. With
-- spring.jpa.hibernate.ddl-auto=validate the app will refuse to start until it is applied by hand.

ALTER TABLE conversation
    ADD COLUMN IF NOT EXISTS user_low_last_read_seq  BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS user_high_last_read_seq BIGINT NOT NULL DEFAULT 0;

-- Backfill existing chats as fully read. New rows default to 0, which is correct for a fresh
-- conversation, but applying that to history would flood every existing chat with a badge for
-- messages the user has almost certainly already seen.
UPDATE conversation
SET user_low_last_read_seq  = last_seq,
    user_high_last_read_seq = last_seq
WHERE user_low_last_read_seq = 0
   OR user_high_last_read_seq = 0;
