-- When each account was created, so the client can mark recent sign-ups as new.
--
-- Nullable, and deliberately not backfilled. Existing rows genuinely have no known creation time,
-- and the two ways to fake one are both worse than admitting that: NOW() would flag every existing
-- account as brand new, and an arbitrary past date would be a fabricated fact in the database.
-- A null reads as "not new", which is the right answer for an account that has been around long
-- enough to predate this column.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS create_date TIMESTAMP(6);
