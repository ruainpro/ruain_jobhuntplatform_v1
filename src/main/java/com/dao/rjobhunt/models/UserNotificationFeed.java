package com.dao.rjobhunt.models;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "user_notification_feeds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationFeed {

    @Id
    private String id;

    private UUID userId;

    @Builder.Default
    private String publicId = UUID.randomUUID().toString();

    private String jobId;        // Job identifier
    private String requestId;    // Request/session ID

    @Builder.Default
    private LocalDateTime notifiedAt = LocalDateTime.now();

    private String jobSource;    // E.g., "simjob", "scrapjob"

    /**  Track how many notifications were sent in a burst or session (e.g., within a job scraping loop) */
    @Builder.Default
    private int notificationCount = 1;

    /** Optional: Daily or session-based rate limitation tracking key */
    private String rateLimitKey; // e.g., "2025-07-12:SIMSCRAPER"

    /**  Whether this entry was part of a capped/rate-limited batch */
    @Builder.Default
    private boolean limited = false;
}