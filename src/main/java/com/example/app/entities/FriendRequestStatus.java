package com.example.app.entities;

/**
 * Persisted as a string (see FriendRequest.status) rather than an ordinal, so adding or reordering
 * a constant here cannot silently reinterpret existing rows.
 */
public enum FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}