package com.dao.rjobhunt.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document(collection = "scraperrequest")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScraperRequest {

    @Id
    @Schema(description = "Internal unique database ID, if stored", example = "65e9b77e2e7d8e3f3f29a1e0")
    private String id;

    @Schema(description = "Public UUID for safely exposing scrape task externally", example = "b8f2bebd-934f-3f29-a126-4b2dfbe1b668")
    private UUID publicId;

    @Schema(description = "User ID of the person who initiated the scrape", example = "dbe4b56c-c11f-44ed-bca2-c76ea4f149a3")
    private UUID userId;

    @Schema(description = "Platform public ID representing which platform configuration is used (e.g., Indeed, LinkedIn)", example = "1")
    private String platformId;

    @NotBlank(message = "Query must not be blank")
    @Size(min = 2, max = 255, message = "Query must be between 2 and 255 characters")
    @Schema(description = "Search query for jobs (e.g., 'software engineer')", example = "software engineer", required = true)
    private String query;

    @NotBlank(message = "Location must not be blank")
    @Size(min = 2, max = 255, message = "Location must be between 2 and 255 characters")
    @Schema(description = "Location for job search (e.g., 'Toronto')", example = "Toronto", required = true)
    private String location;

    @Min(value = 1, message = "maxPages must be at least 1")
    @Max(value = 50, message = "maxPages must not exceed 50")
    @Schema(description = "Number of result pages to scrape. Helps control depth of search.", example = "5", required = false)
    private Integer maxPages = 5;

    @Schema(description = "User-defined profile keywords to personalize job relevance scoring (e.g., ['Java', 'Microservices'])", example = "[\"Java\",\"Spring Boot\"]")
    private List<@NotBlank(message = "Profile keywords must not contain blank values") String> profileKeywords;
    
    private boolean notify; // 👈 Add this field + getter/setter

    @Schema(description = "Optional tags indicating specific preferences or industries of interest (e.g., ['FinTech', 'AI'])", example = "[\"FinTech\",\"AI\"]")
    private List<@NotBlank(message = "Tags must not contain blank values") String> tags;

    @Schema(description = "Custom scraping strategy options as key-value pairs (e.g., {'retryDelay':'2000','maxRetries':'5'})", example = "{\"retryDelay\":\"2000\",\"maxRetries\":\"5\"}")
    private Map<@NotBlank(message = "Custom option keys must not be blank") String, @NotBlank(message = "Custom option values must not be blank") String> customOptions;

    @Schema(description = "Flag indicating whether to enable auto-learning from engagement data", example = "true")
    private Boolean enableAutoLearning = false;
    
    @Schema(description = "Flag indicating whether to enable auto-learning from engagement data", example = "true")
    private Boolean enableAutorun = false;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Optional custom description or context passed with the scrape request", example = "Scraping for special hiring campaign analysis")
    private String description;

    @Schema(description = "Optional custom created date for the scrape request (e.g., planned scrape date)", example = "2025-07-03T10:00:00")
    private Date createdDate;

    @Size(max = 255, message = "Source must not exceed 255 characters")
    @Schema(description = "Optional source identifier, useful for ML experiments or multi-source tracking (e.g., 'LinkedInTestRun')", example = "Indeed")
    private String source;

    @Pattern(regexp = "^$|^https?://.+", message = "URL must be empty or a valid HTTP/HTTPS URL")
    @Schema(description = "Optional target URL for scraping or context; can be a direct job page or search results page", example = "https://ca.indeed.com/jobs?q=software+engineer&l=Toronto")
    private String url;
}
