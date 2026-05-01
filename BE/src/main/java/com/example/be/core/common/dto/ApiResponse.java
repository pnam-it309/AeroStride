package com.example.be.core.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import com.example.be.infrastructure.exceptions.ErrorSeverity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private int status;
    private String message;
    private T data;
    private String code;
    private String error;
    private ErrorSeverity severity;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .code("SUCCESS")
                .message("Operation successful")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String code, String message, String path, ErrorSeverity severity) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .code(code)
                .error(code)
                .severity(severity != null ? severity : ErrorSeverity.RUNTIME)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return error(status, "ERR_UNKNOWN", message, null, ErrorSeverity.RUNTIME);
    }

}
