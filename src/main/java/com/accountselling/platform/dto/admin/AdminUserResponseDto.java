package com.accountselling.platform.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for admin user response information.
 * DTO สำหรับข้อมูลตอบกลับของผู้ใช้สำหรับ admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO containing detailed user information for admin")
public class AdminUserResponseDto {

    @Schema(description = "User ID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID id;

    @Schema(description = "Username", example = "johndoe")
    private String username;

    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Full name", example = "John Doe")
    private String fullName;

    @Schema(description = "User roles", example = "[\"USER\", \"ADMIN\"]")
    private Set<String> roles;

    @Schema(description = "Whether the user account is enabled", example = "true")
    private Boolean enabled;

    @Schema(description = "Total number of orders", example = "15")
    private Integer totalOrders;

    @Schema(description = "Number of completed orders", example = "12")
    private Integer completedOrders;

    @Schema(description = "Number of pending orders", example = "2")
    private Integer pendingOrders;

    @Schema(description = "Number of failed orders", example = "1")
    private Integer failedOrders;

    @Schema(description = "Last login date/time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @Schema(description = "Account creation date/time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Last update date/time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}