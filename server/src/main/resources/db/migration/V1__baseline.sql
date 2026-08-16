-- The core schema: users, posts, comments, likes, refresh tokens.
--
-- Written after the fact. These tables were originally created by Hibernate's ddl-auto, which is
-- why the numbering starts at V2 -- by the time migrations were being written by hand, they already
-- existed. That was survivable locally and fatal anywhere else: a fresh database would run V2
-- (friend_request) first and fail immediately, because it references users(id).
--
-- Reproduced from a schema-only pg_dump of the working development database, so it is what
-- ddl-auto=validate expects. Hibernate validates tables, columns and types -- not constraint names
-- -- so the generated names (uk_r43af9ap4edm43mmtq01oddj6 and friends) are given readable ones here.
--
-- NOTE: Flyway is not on the classpath yet, so this file is not executed automatically.

CREATE TABLE IF NOT EXISTS users (
    id       BIGSERIAL    PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    image    VARCHAR(255),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS post (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(255),
    description TEXT,
    create_date TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS comment (
    id          BIGSERIAL    PRIMARY KEY,
    post_id     BIGINT       NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text        TEXT,
    create_date TIMESTAMP(6)
);

-- Named post_like, not "like" -- LIKE is a reserved word in SQL.
CREATE TABLE IF NOT EXISTS post_like (
    id      BIGSERIAL PRIMARY KEY,
    post_id BIGINT    NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    user_id BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- One like per person per post; the API relies on this to make liking idempotent.
    CONSTRAINT uq_post_like_post_user UNIQUE (post_id, user_id)
);

CREATE TABLE IF NOT EXISTS refresh_token (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token       VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_refresh_token_token UNIQUE (token)
);