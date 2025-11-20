package com.cp.workskillai.util;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;
    private Long timestamp;
    private Integer status;

    // Success static methods
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.OK.value())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.OK.value())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operation completed successfully")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.OK.value())
                .build();
    }

    // Error static methods
    public static <T> ApiResponse<T> error(String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
    }

    public static <T> ApiResponse<T> error(String error, HttpStatus status) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .timestamp(System.currentTimeMillis())
                .status(status.value())
                .build();
    }

    public static <T> ApiResponse<T> error(String error, Integer status) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .timestamp(System.currentTimeMillis())
                .status(status)
                .build();
    }

    // Validation error
    public static <T> ApiResponse<T> validationError(String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .build();
    }

    // Not found
    public static <T> ApiResponse<T> notFound(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(message)
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.NOT_FOUND.value())
                .build();
    }

    // Unauthorized
    public static <T> ApiResponse<T> unauthorized(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(message)
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.UNAUTHORIZED.value())
                .build();
    }

    // Forbidden
    public static <T> ApiResponse<T> forbidden(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(message)
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.FORBIDDEN.value())
                .build();
    }

    // Internal server error
    public static <T> ApiResponse<T> serverError(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(message)
                .timestamp(System.currentTimeMillis())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
    }
}