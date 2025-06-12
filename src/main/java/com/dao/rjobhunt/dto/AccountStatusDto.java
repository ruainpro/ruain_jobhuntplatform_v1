package com.dao.rjobhunt.dto;


import java.time.LocalDateTime;

import com.dao.rjobhunt.models.AccountStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transferring account status data to the frontend,
 * excluding internal persistence logic or database annotations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing the user's account status and verification state")
public class AccountStatusDto {

    @Schema(description = "Account status ID (primary key)", example = "1")
    private Integer accountStatusId;

    @Schema(description = "Status code", example = "0 = inactive, 1 = active, 2 = suspended")
    private Integer statusId;

    @Schema(description = "Optional verification token", example = "a1b2c3d4e5")
    private String token;

    @Schema(description = "Token generation timestamp", example = "2025-06-10T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Token expiration timestamp", example = "2025-06-10T15:30:00")
    private LocalDateTime expiresAt;

    public static AccountStatusDto fromEntity(AccountStatus status) {
        return new AccountStatusDto(
            status.getAccountStatusId(),
            status.getStatusId(),
            status.getToken(),
            status.getCreatedAt(),
            status.getExpiresAt()
        );
    }
}