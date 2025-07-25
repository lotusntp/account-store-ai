package com.accountselling.platform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that disables most security features for unit testing.
 * This configuration is used in @WebMvcTest to allow testing controller logic
 * without dealing with authentication complexities.
 * 
 * การตั้งค่า security สำหรับการทดสอบที่ปิดการใช้งาน security ส่วนใหญ่
 * ใช้ใน @WebMvcTest เพื่อให้สามารถทดสอบ logic ของ controller ได้
 * โดยไม่ต้องจัดการกับความซับซ้อนของ authentication
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/payments/webhook").permitAll() // Allow webhook without auth
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}