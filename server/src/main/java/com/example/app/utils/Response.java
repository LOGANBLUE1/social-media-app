package com.example.app.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * The response envelope every endpoint returns. Handlers return it directly and declare their
 * status with @ResponseStatus, so no ResponseEntity<Response<T>> nesting appears in signatures;
 * anything other than the happy path is thrown and rendered by GlobalExceptionHandler.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response<T> {
    private final boolean success;
    private final String message;
    private final T data;

    private Response(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> Response<T> success(T data) {
        return new Response<>(true, null, data);
    }

    public static <T> Response<T> success(T data, String message) {
        return new Response<>(true, message, data);
    }

    public static <T> Response<T> error(String message) {
        return new Response<>(false, message, null);
    }

    // status.is2xxSuccessful() -> returns true if status in range [200-299]
    public static <T> Response<T> of(HttpStatus status, String message, T data) {
        return new Response<>(status.is2xxSuccessful(), message, data);
    }
}