package com.dao.rjobhunt.Controller.authentication;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

import com.dao.rjobhunt.Security.JwtService;
import com.dao.rjobhunt.Service.ActionHistoryServices;
import com.dao.rjobhunt.Service.UserServices;
import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.dto.AuthRequest;
import com.dao.rjobhunt.dto.AuthResponse;
import com.dao.rjobhunt.dto.UserDto;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.repository.UserInfoRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private UserServices userService;

    @Autowired
    private UserInfoRepository infoRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ActionHistoryServices actionHistoryServices;

    @Value("${admin.secret.code}")
    private String adminCodeSecret;

    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome this endpoint is not secure";
    }

    @PostMapping("/addNewUser")
    public ResponseEntity<ApiResponse<UserDto>> registerUser(@Valid @RequestBody UserDto userDto) {
        try {
            userDto.setRole("ROLE_USER");
            UserDto dto = userService.registerUser(userDto);
            actionHistoryServices.addActionHistory(null, dto.getEmail() + ":User Registration");
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Something went wrong"));
        }
    }

    @PostMapping("/addNewAdmin")
    public ResponseEntity<ApiResponse<UserDto>> registerAdmin(
            @Valid @RequestBody UserDto userDto,
            @RequestHeader("X-Admin-Code") String adminCode) {
        try {
            if (!adminCodeSecret.equals(adminCode)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Invalid admin code"));
            }
            userDto.setRole("ROLE_ADMIN");
            UserDto dto = userService.registerUser(userDto);
            actionHistoryServices.addActionHistory(null, dto.getEmail() + ":Admin Registration");
            return ResponseEntity.ok(ApiResponse.success("Admin registered successfully", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Something went wrong"));
        }
    }

    @Operation(summary = "Verify user email using token",
            description = "Verifies a user account using the token sent via email. The token must be valid and not expired (24-hour limit).")
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<UserDto>> verifyUserByToken(
            @Parameter(description = "Verification token from email", required = true)
            @RequestParam("token") String token) {
        try {
            boolean verifiedUser = userService.verifyAccountByToken(token);
            return ResponseEntity.ok(ApiResponse.success("User verified successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Something went wrong"));
        }
    }

    @Operation(summary = "Authenticate user and return JWT token + role")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody AuthRequest request,
            HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            if (!authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Invalid credentials"));
            }

            User user = infoRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            Integer status = user.getAccountStatus() != null ? user.getAccountStatus().getStatusId() : 0;

            if (status != 1) {
                String message = switch (status) {
                    case 0 -> "Account deactivated. Please check your email.";
                    case 2 -> "Account suspended. Contact support.";
                    default -> "Account inactive.";
                };
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(message));
            }

            String token = jwtService.generateTokenWithClaims(
                    user.getEmail(), Map.of("role", user.getRole())
            );

            ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(Duration.ofHours(3))
                    .build();

            response.setHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

            AuthResponse res = new AuthResponse(user.getPublicId(), user.getEmail(), token, user.getRole());
            actionHistoryServices.addActionHistory(user.getUserId(), "User Login");

            return ResponseEntity.ok(ApiResponse.success("Login successful", res));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        ResponseCookie clearedCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, clearedCookie.toString());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestParam String email) {
        try {
            userService.generateAndSendNewPassword(email);
            actionHistoryServices.addActionHistory(null, email + ":User forgotPassword");
            return ResponseEntity.ok(ApiResponse.success("New password has been sent to your email", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Something went wrong"));
        }
    }

    @PostMapping("/generateToken")
    public String authenticateAndGetToken(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
        );
        if (authentication.isAuthenticated()) {
            actionHistoryServices.addActionHistory(null, authRequest.getEmail() + ":Token Generation");
            return jwtService.generateToken(authRequest.getEmail());
        } else {
            throw new UsernameNotFoundException("Invalid user request!");
        }
    }
}
