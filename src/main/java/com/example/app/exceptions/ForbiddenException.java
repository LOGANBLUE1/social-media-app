package com.example.app.exceptions;

/**
 * The caller is authenticated but not allowed to act on this particular row -- accepting someone
 * else's friend request, cancelling a request they did not send. Distinct from
 * UnauthorizedException, which means "we do not know who you are".
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}