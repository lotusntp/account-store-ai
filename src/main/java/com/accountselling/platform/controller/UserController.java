package com.accountselling.platform.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

  @GetMapping("/profile")
  public ResponseEntity<Map<String, String>> getUserProfile() {
    // This is just a placeholder for the test
    // In a real implementation, this would return the authenticated user's profile
    Map<String, String> response = new HashMap<>();
    response.put("message", "User profile endpoint");
    return ResponseEntity.ok(response);
  }
}
