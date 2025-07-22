package com.accountselling.platform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> login() {
        // This is just a placeholder for the test
        // In a real implementation, this would be a POST endpoint that accepts credentials
        Map<String, String> response = new HashMap<>();
        response.put("message", "Login endpoint");
        return ResponseEntity.ok(response);
    }
}