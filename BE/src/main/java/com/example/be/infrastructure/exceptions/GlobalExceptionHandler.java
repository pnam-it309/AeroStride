package com.example.be.infrastructure.exceptions;

import com.example.be.core.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("Business Warning at {}: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "ERR_BUSINESS_LOGIC", e.getMessage(), request.getRequestURI(), ErrorSeverity.RECOVERABLE);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(ValidationException e, HttpServletRequest request) {
        log.warn("Validation Warning at {}: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(e.getStatus(), e.getErrorCode(), e.getMessage(), request.getRequestURI(), ErrorSeverity.SYNTAX);
    }

    @ExceptionHandler(SystemException.class)
    public ResponseEntity<ApiResponse<Object>> handleSystemException(SystemException e, HttpServletRequest request) {
        log.error("SYSTEM CRITICAL at {}: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(e.getStatus(), e.getErrorCode(), "A system error occurred. Please try again later.", request.getRequestURI(), ErrorSeverity.FATAL);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {
        log.warn("Not Found at {}: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "ERR_NOT_FOUND", e.getMessage(), request.getRequestURI(), ErrorSeverity.RECOVERABLE);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(DuplicateResourceException e, HttpServletRequest request) {
        log.warn("Duplicate resource at {}: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "ERR_DUPLICATE_RESOURCE", e.getMessage(), request.getRequestURI(), ErrorSeverity.RECOVERABLE);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(UnauthorizedException e, HttpServletRequest request) {
        log.warn("Unauthorized access at {}: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "ERR_UNAUTHORIZED", "Vui lòng đăng nhập để tiếp tục.", request.getRequestURI(), ErrorSeverity.RECOVERABLE);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameNotFoundException(UsernameNotFoundException e, HttpServletRequest request) {
        log.warn("User not found at {}: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "ERR_USER_NOT_FOUND", "Tài khoản (Email) không tồn tại trong hệ thống.", request.getRequestURI(), ErrorSeverity.RECOVERABLE);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
        log.warn("Wrong password attempt at {}: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "ERR_WRONG_PASSWORD", "Mật khẩu không chính xác. Vui lòng kiểm tra lại.", request.getRequestURI(), ErrorSeverity.RECOVERABLE);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("Access Denied at {}: {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "ERR_FORBIDDEN", "You do not have permission to access this resource.", request.getRequestURI(), ErrorSeverity.RECOVERABLE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.warn("Validation error at {}: {}", request.getRequestURI(), message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "ERR_VAL_INVALID_PARAMS", message, request.getRequestURI(), ErrorSeverity.SYNTAX);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception e, HttpServletRequest request) {
        log.error("UNHANDLED CRITICAL at {}: ", request.getRequestURI(), e);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_INTERNAL_SERVER", 
                "An unexpected server error occurred. Our engineers have been notified.", request.getRequestURI(), ErrorSeverity.RUNTIME);
    }

    private ResponseEntity<ApiResponse<Object>> buildErrorResponse(HttpStatus status, String code, String message, String path, ErrorSeverity severity) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status.value(), code, message, path, severity));
    }



}
