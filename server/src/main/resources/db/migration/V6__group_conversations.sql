-- Generalises conversation from "exactly two people" to "a set of people", so group chats reuse
-- the whole existing pipeline -- messages, seq ordering, unread counts -- instead of duplicating it.
--
-- A 1:1 chat becomes a conversation with two participants and no name. That is the model Slack,
-- Discord and Matrix all settled on, and the reason is that everything downstream of the room
-- (send, list, mark read, later: reactions, attachments, search) stops caring how many people are
-- in it. The alternative -- a parallel set of group_* tables -- doubles every one of those paths.
--
-- Membership moves OUT of the conversation row entirely. It previously lived in two places at once:
-- the normalised (user_low_id, user_high_id) pair, and the two read-position columns beside it.
-- Both are replaced by conversation_participant, so "who is in this room and how far have they
-- read" has exactly one answer.
--
-- Uniqueness for direct chats is preserved by direct_key: the two user ids, smaller first, as
-- "3:7". Same guarantee the old unique pair gave, one indexed lookup, and no user columns on the
-- conversation. Null for groups, and Postgres permits many nulls in a unique index, so groups
-- never collide.

ALTER TABLE conversation
    ADD COLUMN IF NOT EXISTS type       VARCHAR(16),
    ADD COLUMN IF NOT EXISTS name       VARCHAR(120),
    ADD COLUMN IF NOT EXISTS owner_id   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS direct_key VARCHAR(64);

-- Everything that exists today is a 1:1 chat.
UPDATE conversation SET type = 'DIRECT' WHERE type IS NULL;
UPDATE conversation
SET direct_key = LEAST(user_low_id, user_high_id) || ':' || GREATEST(user_low_id, user_high_id)
WHERE direct_key IS NULL
  AND user_low_id IS NOT NULL;

ALTER TABLE conversation ALTER COLUMN type SET NOT NULL;

CREATE TABLE IF NOT EXISTS conversation_participant (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT    NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    user_id         BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- How far this person has read, as a seq. Same meaning the two columns on conversation had;
    -- the unread count is still lastSeq minus this, with no COUNT over message.
    last_read_seq   BIGINT    NOT NULL DEFAULT 0,
    join_date       TIMESTAMP,
    CONSTRAINT uq_conversation_participant UNIQUE (conversation_id, user_id)
);

-- The chat list is "conversations where I am a participant, newest first", so it starts here.
CREATE INDEX IF NOT EXISTS idx_conversation_participant_user
    ON conversation_participant (user_id);

-- Carry both sides of every existing conversation across, read positions included, before the
-- columns holding them are dropped below.
INSERT INTO conversation_participant (conversation_id, user_id, last_read_seq, join_date)
SELECT id, user_low_id, user_low_last_read_seq, create_date
FROM conversation
WHERE user_low_id IS NOT NULL
ON CONFLICT (conversation_id, user_id) DO NOTHING;

INSERT INTO conversation_participant (conversation_id, user_id, last_read_seq, join_date)
SELECT id, user_high_id, user_high_last_read_seq, create_date
FROM conversation
WHERE user_high_id IS NOT NULL
ON CONFLICT (conversation_id, user_id) DO NOTHING;

-- Now that membership and read state are copied, the pair representation goes. This is the
-- destructive step: after it, the old columns cannot be reconstructed except from a backup.
ALTER TABLE conversation
    DROP CONSTRAINT IF EXISTS uq_conversation_pair,
    DROP CONSTRAINT IF EXISTS ck_conversation_ordered;

ALTER TABLE conversation
    DROP COLUMN IF EXISTS user_low_id,
    DROP COLUMN IF EXISTS user_high_id,
    DROP COLUMN IF EXISTS user_low_last_read_seq,
    DROP COLUMN IF EXISTS user_high_last_read_seq;

CREATE UNIQUE INDEX IF NOT EXISTS uq_conversation_direct_key
    ON conversation (direct_key);

-- The chat list sorts on this; the old per-user indexes went with the columns above.
CREATE INDEX IF NOT EXISTS idx_conversation_last_message
    ON conversation (last_message_at DESC);

-- Keeps the two shapes honest: a direct chat is keyed and unnamed, a group is named and unkeyed.
ALTER TABLE conversation
    ADD CONSTRAINT ck_conversation_shape CHECK (
        (type = 'DIRECT' AND direct_key IS NOT NULL AND name IS NULL)
     OR (type = 'GROUP'  AND direct_key IS NULL     AND name IS NOT NULL)
    );
