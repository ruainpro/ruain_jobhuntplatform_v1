package com.dao.rjobhunt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Login request with email and password")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @Schema(description = "User email", example = "john2.doe@example.com", required = true)
    private String email;

    @Schema(description = "User password", example = "StrongPassword123", required = true)
    private String password;
}