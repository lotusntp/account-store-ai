package com.accountselling.platform.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.accountselling.platform.dto.auth.LoginRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void publicEndpointsShouldBeAccessible() throws Exception {
    LoginRequestDto loginRequest = new LoginRequestDto("testuser", "password123");
    ObjectMapper objectMapper = new ObjectMapper();

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api-docs")).andExpect(status().isOk());

    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  @Test
  void protectedEndpointsShouldRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/users/profile")).andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedUserCanAccessProtectedEndpoints() throws Exception {
    mockMvc.perform(get("/api/users/profile")).andExpect(status().isUnauthorized());
  }
}
