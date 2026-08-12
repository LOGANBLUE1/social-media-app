-- Friend requests, and by extension friendships: a PENDING row is a request, an ACCEPTED row is
-- the friendship. Directional (requester asked addressee) even though friendship is symmetric.
--
-- NOTE: Flyway is not on the classpath yet, so this file is not executed automatically. It is
-- written as an idempotent Flyway migration so that adding the dependency later needs no edits.

CREATE TABLE IF NOT EXISTS friend_request (
    id           BIGSERIAL   PRIMARY KEY,
    requester_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    addressee_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status       VARCHAR(16) NOT NULL,
    create_date  TIMESTAMP,
    respond_date TIMESTAMP,
    CONSTRAINT uq_friend_request_pair    UNIQUE (requester_id, addressee_id),
    CONSTRAINT ck_friend_request_no_self CHECK (requester_id <> addressee_id)
);

CREATE INDEX IF NOT EXISTS idx_friend_request_addressee ON friend_request (addressee_id, status);
CREATE INDEX IF NOT EXISTS idx_friend_request_requester ON friend_request (requester_id, status);
