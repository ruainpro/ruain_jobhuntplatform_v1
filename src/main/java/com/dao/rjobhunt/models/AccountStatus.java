package com.dao.rjobhunt.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "accountStatus")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Account status with optional verification token data")
public class AccountStatus {

    @Id
    @Schema(description = "MongoDB document ID or status ID", example = "1")
    private Integer accountStatusId;

    @Schema(description = "Internal system status code", example = "0,1,2,3")
    private Integer statusId;

    @Schema(description = "Verification token string", example = "a1b2c3d4e5")
    private String token;
    
    @Schema(description = "Verification token for what verify or forgot", example = "Verify or forgot")
    private String tokenType;

    @Schema(description = "Token creation timestamp", example = "2025-06-10T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Token expiry timestamp", example = "2025-06-10T15:30:00")
    private LocalDateTime expiresAt;
    
    @Schema(description = "request count", example = "1,2,3")
    private int RequestCount;

}
