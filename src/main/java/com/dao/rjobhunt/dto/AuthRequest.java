package com.dao.rjobhunt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Login request with email and password")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @Schema(description = "User email", example = "user@example.com", required = true)
    private String email;

    @Schema(description = "User password", example = "StrongPassword123!", required = true)
    private String password;
}