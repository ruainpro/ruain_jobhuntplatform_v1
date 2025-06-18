package com.dao.rjobhunt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testLoginReturnsTokenAndLogoutClearsCookie() throws Exception {
        // Login Request
        Map<String, String> loginPayload = Map.of(
                "email", "ruainpro@gmail.com",
                "password", "IAMrup"
        );

        MvcResult loginResult = mockMvc.perform(
                        post("/auth/login")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("ruainpro@gmail.com"))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        // Extract token from response body
        String responseContent = loginResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseContent);
        String jwtToken = root.path("data").path("token").asText();
        assertThat(jwtToken).startsWith("eyJ");

        // Extract cookie if present
        String setCookieHeader = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).isNotNull();

        // Logout Request using cookie
        MvcResult logoutResult = mockMvc.perform(
                        post("/auth/logout")
                                .header("Cookie", setCookieHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andReturn();

        String logoutCookieHeader = logoutResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(logoutCookieHeader).contains("jwt=");
        assertThat(logoutCookieHeader).contains("Max-Age=0");
    }
}