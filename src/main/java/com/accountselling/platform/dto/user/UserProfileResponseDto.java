package com.accountselling.platform.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for user profile response.
 * Contains comprehensive user profile information for authenticated users.
 * 
 * DTO สำหรับการตอบกลับข้อมูลโปรไฟล์ผู้ใช้
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDto {

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Set<String> roles;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Order-related statistics
    private Integer totalOrders;
    private Integer completedOrders;
    private Integer pendingOrders;
}