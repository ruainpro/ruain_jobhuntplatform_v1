package com.dao.rjobhunt;

import com.dao.rjobhunt.Controller.authentication.UserController;
import com.dao.rjobhunt.Service.ActionHistoryServices;
import com.dao.rjobhunt.dto.AuthRequest;
import com.dao.rjobhunt.models.AccountStatus;
import com.dao.rjobhunt.models.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private ActionHistoryServices actionHistoryServices;
    @MockBean private com.dao.rjobhunt.Service.UserServices userServices;
    @MockBean private com.dao.rjobhunt.Service.UserInfoService userInfoService;
    @MockBean private com.dao.rjobhunt.Security.JwtService jwtService;
    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private com.dao.rjobhunt.repository.UserInfoRepository userInfoRepository;

    private final String validEmail = "ruainpro@gmail.com";
    private final String validPassword = "IAMrup";
    private User mockUser;

    @BeforeEach
    public void setup() {
        mockUser = new User();
        mockUser.setEmail(validEmail);
        mockUser.setPassword(validPassword);
        mockUser.setRole("ROLE_USER");
        mockUser.setUserId("60e1161d-b7be-49cf-a004-633b78016c20");
        mockUser.setPublicId(UUID.randomUUID());

        AccountStatus accountStatus = new AccountStatus();
        accountStatus.setStatusId(1);

        accountStatus.setCreatedAt(LocalDateTime.now());
        accountStatus.setExpiresAt(LocalDateTime.now().plusDays(1));
        mockUser.setAccountStatus(accountStatus);
    }

    @Test
    public void testLoginWithValidCredentials_shouldReturnSuccess() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail(validEmail);
        authRequest.setPassword(validPassword);

        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(mockAuth.isAuthenticated()).thenReturn(true);

        when(userInfoRepository.findByEmail(validEmail)).thenReturn(Optional.of(mockUser));
        when(jwtService.generateTokenWithClaims(any(), anyMap())).thenReturn("mock-jwt-token");
        when(actionHistoryServices.addActionHistory(eq(mockUser.getUserId()), eq("User Login"))).thenReturn(true);

        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.email").value(validEmail));
    }

    @Test
    public void testLoginWithInvalidCredentials_shouldReturnUnauthorized() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setEmail(validEmail);
        authRequest.setPassword("wrongpass");

        when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("Auth failed"));

        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
    }
}
