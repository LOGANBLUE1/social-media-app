package com.example.app.entities;

/**
 * What kind of room a conversation is.
 *
 * DIRECT is not a separate thing from GROUP so much as a constrained one: two participants, no
 * name, and a direct_key that makes the pair unique. Everything past the participant list --
 * messages, seq ordering, read positions -- behaves identically for both.
 */
public enum ConversationType {
    DIRECT,
    GROUP
}
