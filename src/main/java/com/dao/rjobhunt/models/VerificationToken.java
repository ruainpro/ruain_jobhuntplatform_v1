package com.dao.rjobhunt.models;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Model representing a verification token with its timestamps.")
public class VerificationToken {


    @Schema(description = "Verification token string", example = "a1b2c3d4e5")
    private String token;

    @Schema(description = "Token creation timestamp", example = "2025-06-10T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Token expiry timestamp", example = "2025-06-10T15:30:00")
    private LocalDateTime expiresAt;

}
