package com.dao.rjobhunt.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "platforms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Platform", description = "Represents a scraping platform configuration")
public class Platform {

    @Id
//    @JsonIgnore
    @Schema(hidden = true, description = "MongoDB internal platform ID")
    private String platformId;

    @Schema(description = "Public UUID for external reference")
    private UUID publicId;

    @Indexed(unique = true)
    @Schema(description = "Name of the platform, e.g., Indeed or LinkedIn")
    private String name;

    @Indexed(unique = true)
    @Schema(description = "Base URL for the platform")
    private String url;

    @Schema(description = "URL template with placeholders like {{QUERY}} and {{LOCATION}}")
    private String urlTemplate;

    @Schema(description = "Type of platform, e.g., indeed, linkedin, custom")
    private String type;  // Flexible type as plain string

    @Schema(description = "Current status of the scraper for this platform")
    private String scraperStatus;

    @Schema(description = "Weight to prioritize this platform when scraping")
    private int preferenceWeight;

    @NotNull
    @Schema(description = "Which parser to use for this platform")
    private ParserType parserType;

    @Schema(description = "Whether this platform is currently active for scraping")
    private Boolean isActive;

    @Schema(description = "Optional notes about this platform")
    private String notes;

    @Schema(description = "API key is api platform used")
    private String apiKey;
    
    
    @Schema(description = "ID of the user who created this platform; null for admin-created platforms")
    private String createdByUserId;

//    @JsonIgnore
    @Schema(hidden = true, description = "CSS selectors map for scraping; internal only")
    private Map<String, String> selectors;

    @Schema(description = "Detection configuration YAML content for advanced field detection")
    private String detectionYaml;  // ❗️ Removed @JsonIgnore so it is included in API input/output

    @CreatedDate
    @Schema(description = "Platform creation timestamp")
    private LocalDateTime createdDate;

    @JsonIgnore
    @Schema(hidden = true, description = "Platform last updated timestamp")
    private LocalDateTime lastUpdated;
}