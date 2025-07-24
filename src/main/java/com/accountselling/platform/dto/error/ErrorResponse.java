package com.accountselling.platform.dto.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standard error response record for all API endpoints.
 * Provides consistent error information structure throughout the application.
 * 
 * Error response มาตรฐานสำหรับทุก API endpoint ในระบบ
 */
@Schema(description = "Standard error response format")
public record ErrorResponse(
    
    @Schema(description = "HTTP status code", example = "400")
    int status,
    
    @Schema(description = "HTTP status text", example = "Bad Request")
    String error,
    
    @Schema(description = "Detailed error message", example = "Username cannot be blank")
    String message,
    
    @Schema(description = "Request path that caused the error", example = "/api/auth/register")
    String path,
    
    @Schema(description = "Timestamp when error occurred")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    Instant timestamp
) {
    
    /**
     * Create ErrorResponse with current timestamp.
     * 
     * @param status HTTP status code
     * @param error HTTP status text  
     * @param message detailed error message
     * @param path request path
     * @return ErrorResponse with current timestamp
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now());
    }
    
    /**
     * Create ErrorResponse for bad request (400).
     * 
     * @param message detailed error message
     * @param path request path
     * @return ErrorResponse for bad request
     */
    public static ErrorResponse badRequest(String message, String path) {
        return of(400, "Bad Request", message, path);
    }
    
    /**
     * Create ErrorResponse for unauthorized (401).
     * 
     * @param message detailed error message  
     * @param path request path
     * @return ErrorResponse for unauthorized
     */
    public static ErrorResponse unauthorized(String message, String path) {
        return of(401, "Unauthorized", message, path);
    }
    
    /**
     * Create ErrorResponse for forbidden (403).
     * 
     * @param message detailed error message
     * @param path request path  
     * @return ErrorResponse for forbidden
     */
    public static ErrorResponse forbidden(String message, String path) {
        return of(403, "Forbidden", message, path);
    }
    
    /**
     * Create ErrorResponse for not found (404).
     * 
     * @param message detailed error message
     * @param path request path
     * @return ErrorResponse for not found
     */
    public static ErrorResponse notFound(String message, String path) {
        return of(404, "Not Found", message, path);
    }
    
    /**
     * Create ErrorResponse for conflict (409).
     * 
     * @param message detailed error message
     * @param path request path
     * @return ErrorResponse for conflict
     */
    public static ErrorResponse conflict(String message, String path) {
        return of(409, "Conflict", message, path);
    }
    
    /**
     * Create ErrorResponse for internal server error (500).
     * 
     * @param message detailed error message
     * @param path request path
     * @return ErrorResponse for internal server error
     */
    public static ErrorResponse internalServerError(String message, String path) {
        return of(500, "Internal Server Error", message, path);
    }
}