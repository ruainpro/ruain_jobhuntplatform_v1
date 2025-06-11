package com.dao.rjobhunt.Controller.authentication;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.dao.rjobhunt.Security.JwtService;
import com.dao.rjobhunt.Service.UserInfoService;
import com.dao.rjobhunt.Service.UserServices;
import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.dto.AuthRequest;
import com.dao.rjobhunt.dto.AuthResponse;
import com.dao.rjobhunt.dto.UserDto;
import com.dao.rjobhunt.models.AccountStatus;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.models.VerificationToken;
import com.dao.rjobhunt.repository.UserInfoRepository;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

	@Autowired
    private UserInfoService service;
	
	@Autowired
	private UserServices userService;
	
	@Autowired
	private UserInfoRepository infoRepository;

	@Autowired
    private JwtService jwtService;

	@Autowired
    private AuthenticationManager authenticationManager;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome this endpoint is not secure";
    }


    @PostMapping("/addNewUser")
    public ResponseEntity<ApiResponse<UserDto>> register(@RequestBody User userMd) {
        try {
            UserDto dto = userService.registerUser(userMd);
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Something went wrong"));
        }
    }
//    @GetMapping("/verify")
//    public ResponseEntity<ApiResponse<String>> verify(
//            @RequestParam String token,
//            @RequestParam UUID publicId) {
//
//        User user = infoRepository.findByPublicId(publicId)
//            .orElseThrow(() -> new RuntimeException("User not found"));
//
//        VerificationToken vt = user.getVerificationToken();
//
//        if (vt == null || !vt.getToken().equals(token)) {
//            return ResponseEntity.badRequest().body(ApiResponse.fail("Invalid token"));
//        }
//
//        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
//            return ResponseEntity.badRequest().body(ApiResponse.fail("Token expired"));
//        }
//
//        user.setAccountStatus(1); // Activate
//        user.setVerificationToken(null); // Optional cleanup
//        infoRepository.save(user);
//
//        return ResponseEntity.ok(ApiResponse.success("Account verified", null));
//    }

    
    
    @Operation(summary = "Authenticate user and return JWT token + role")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            if (!authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Invalid credentials"));
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
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail(message));
            }

            // ✅ Embed role into JWT
            String token = jwtService.generateTokenWithClaims(
            	    user.getEmail(),
            	    Map.of("role", user.getRole()) // e.g., "ROLE_ADMIN"
            	);
            
            //            String token = jwtService.generateToken(request.getEmail());

            AuthResponse response = new AuthResponse(
                user.getPublicId(),
                user.getEmail(),
                token,
                user.getRole()
            );

            return ResponseEntity.ok(ApiResponse.success("Login successful", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("Login failed: " + e.getMessage()));
        }
    }


    
    

    // Removed the role checks here as they are already managed in SecurityConfig

//    @PostMapping("/generateToken")
//    public String authenticateAndGetToken(@RequestBody AuthRequest authRequest) {
//        Authentication authentication = authenticationManager.authenticate(
//            new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
//        );
//        if (authentication.isAuthenticated()) {
//            return jwtService.generateToken(authRequest.getUsername());
//        } else {
//            throw new UsernameNotFoundException("Invalid user request!");
//        }
//    }
}