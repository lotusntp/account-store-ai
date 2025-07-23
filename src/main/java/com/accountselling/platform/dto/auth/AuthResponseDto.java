package com.accountselling.platform.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for authentication response.
 * Contains JWT tokens and user information after successful authentication.
 * 
 * DTO สำหรับการตอบกลับการยืนยันตัวตน รวมถึง JWT tokens และข้อมูลผู้ใช้
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private String accessToken;
    private String refreshToken;
    @Builder.Default  
    private String tokenType = "Bearer";
    private Long accessTokenExpiresIn;
    private Long refreshTokenExpiresIn;
    
    private UserInfoDto user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDto {
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String fullName;
        private Set<String> roles;
        private Boolean enabled;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
    }
}