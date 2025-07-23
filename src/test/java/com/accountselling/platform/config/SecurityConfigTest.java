package com.accountselling.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpointsShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/auth/login")).andExpect(status().isOk());

        mockMvc.perform(get("/api-docs")).andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void authenticatedUserCanAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/users/profile")).andExpect(status().isOk());
    }
}
