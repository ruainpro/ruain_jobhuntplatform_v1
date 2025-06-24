package com.dao.rjobhunt.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Transient;


import java.time.Instant;

@Document(collection = "action_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionHistory {

    @Id
    @JsonIgnore
    private String id;

    @Schema(description = "Public-safe UUID", example = "55c9c254-2ad1-4c96-a578-1e43e23c7be5", required = true)
    private String publicId;

    @Schema(hidden = true)
    @JsonIgnore
    private String userId;

    @NotBlank
    @Schema(description = "Type of action (e.g. LOGIN, APPLY_JOB)", example = "LOGIN", required = true)
    private String actionType;

    @NotBlank
    @Schema(description = "Entity acted upon (e.g. USER, JOB)", example = "USER", required = true)
    private String actionEntity;

//    @Schema(hidden = true)
//    private String entityId;

    @NotBlank
    @Schema(description = "Description of the action", example = "User logged in successfully", required = true)
    private String description;

    @Schema(description = "Timestamp of the action", example = "2025-06-22T14:22:11Z", required = true)
    @CreatedDate
    private Instant timestamp;

    @Schema(description = "IP address of the requester", example = "192.168.0.1")
    private String ipAddress;

    @Schema(description = "Browser and device information", example = "Chrome on Windows 11")
    private String deviceInfo;
    
    @Transient
    private String userEmail;
    
    @Transient
    private String userRole;
    
    @Transient
    private String userPublicId;
}