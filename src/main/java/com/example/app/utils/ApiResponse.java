package com.example.app.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    public static <T> ResponseEntity<ApiResponse<T>> apiResponseEntity(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status).body(new ApiResponse<T>(status.is2xxSuccessful(), message, data));
    }
    public static  ResponseEntity apiResponseEntity(HttpStatus status) {
        return ResponseEntity.status(status).build();
    }

    public static <T> ResponseEntity<ApiResponse<T>> serverFailure() {
        return apiResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", null);
    }
    public static <T> ResponseEntity<ApiResponse<T>>  failure(HttpStatus status, String message) {
        return apiResponseEntity(status, message, null);
    }
    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return apiResponseEntity(HttpStatus.OK, null, data);
    }
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return apiResponseEntity(HttpStatus.CREATED, null, data);
    }
    public static ResponseEntity deleted() {
        return apiResponseEntity(HttpStatus.NO_CONTENT);
    }
    public static <T> ResponseEntity<ApiResponse<T>> build(HttpStatus status, String message, T data) {
        return apiResponseEntity(status, message, data);
    }
}