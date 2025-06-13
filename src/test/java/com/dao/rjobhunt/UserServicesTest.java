package com.dao.rjobhunt;

import com.dao.rjobhunt.Service.EmailService;
import com.dao.rjobhunt.Service.UserServices;
import com.dao.rjobhunt.dto.UserDto;
import com.dao.rjobhunt.models.AccountStatus;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.others.RequestUtil;
import com.dao.rjobhunt.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServicesTest {

    @InjectMocks
    private UserServices userServices;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private RequestUtil requestUtil;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = User.builder()
                .email("test@example.com")
                .password("oldpass")
                .role("ROLE_USER")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .accountStatus(AccountStatus.builder()
                        .accountStatusId(1)
                        .statusId(0)
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusHours(24))
                        .token("abc123")
                        .tokenType("verify")
                        .build())
                .build();

        userDto = new UserDto();
        userDto.setEmail("test@example.com");
        userDto.setPassword("rawpass");
        userDto.setRole("ROLE_USER");
    }

    @Test
    void testRegisterUser_Success() {
        when(userInfoRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("rawpass")).thenReturn("encodedpass");
        when(userInfoRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setRole("ROLE_USER");
            return u;
        });
        when(templateEngine.process(eq("email/verify-email"), any(Context.class))).thenReturn("<html>Email</html>");

        UserDto result = userServices.registerUser(userDto);

        assertEquals("test@example.com", result.getEmail());
        assertEquals("ROLE_USER", result.getRole());
        assertNotNull(result.getAccountStatus());
    }

    @Test
    void testVerifyAccountByToken_Success() {
        when(userInfoRepository.findByAccountStatus_Token("abc123")).thenReturn(Optional.of(user));
        when(userInfoRepository.save(any(User.class))).thenReturn(user);

        boolean result = userServices.verifyAccountByToken("abc123");
        assertTrue(result);
    }

    @Test
    void testGenerateAndSendNewPassword_Success() {
        when(userInfoRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(requestUtil.generateRandomPassword(10)).thenReturn("newpass123");
        when(passwordEncoder.encode("newpass123")).thenReturn("encodedpass");
        when(templateEngine.process(eq("email/forgot_password"), any(Context.class))).thenReturn("<html>Reset</html>");
        when(userInfoRepository.save(any(User.class))).thenReturn(user);

        User updatedUser = userServices.generateAndSendNewPassword("test@example.com");

        assertEquals("encodedpass", updatedUser.getPassword());
        assertNotNull(updatedUser.getUpdatedAt());
    }

    @Test
    void testRegisterUser_EmailExists_ThrowsException() {
        when(userInfoRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        assertThrows(IllegalArgumentException.class, () -> userServices.registerUser(userDto));
    }

    @Test
    void testVerifyAccountByToken_AlreadyVerified() {
        user.getAccountStatus().setStatusId(1);
        when(userInfoRepository.findByAccountStatus_Token("abc123")).thenReturn(Optional.of(user));
        assertThrows(IllegalArgumentException.class, () -> userServices.verifyAccountByToken("abc123"));
    }

    @Test
    void testVerifyAccountByToken_ExpiredToken() {
        user.getAccountStatus().setCreatedAt(LocalDateTime.now().minusHours(25));
        when(userInfoRepository.findByAccountStatus_Token("abc123")).thenReturn(Optional.of(user));
        assertThrows(IllegalArgumentException.class, () -> userServices.verifyAccountByToken("abc123"));
    }

    @Test
    void testGenerateAndSendNewPassword_UserNotFound() {
        when(userInfoRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userServices.generateAndSendNewPassword("missing@example.com"));
    }
}

