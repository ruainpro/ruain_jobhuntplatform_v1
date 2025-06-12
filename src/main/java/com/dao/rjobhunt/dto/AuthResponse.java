package com.dao.rjobhunt.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "JWT response with user role")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID uid;
    private String email;
    private String token;

    @Schema(description = "User role", example = "ROLE_USER")
    private String role;
}