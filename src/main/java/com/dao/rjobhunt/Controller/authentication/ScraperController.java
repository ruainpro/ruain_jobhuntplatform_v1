package com.dao.rjobhunt.Controller.authentication;

import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.models.Job;
import com.dao.rjobhunt.models.ScraperRequest;
import com.dao.rjobhunt.Security.JwtService;
import com.dao.rjobhunt.Service.ActionHistoryServices;
import com.dao.rjobhunt.Service.ScraperService;
import com.dao.rjobhunt.Service.SseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;

@Slf4j
@RestController
@RequestMapping("/api/scraper")
@RequiredArgsConstructor
@Tag(name = "Scraper API", description = "Endpoints for managing scraping tasks and ScraperRequests")
public class ScraperController {

    @Autowired
    private ScraperService scraperService;

    @Autowired
    private ActionHistoryServices actionHistoryServices;

    private final SseService sseService;

    @Autowired
    private JwtService jwtService;

    // =======================
    // SCRAPING CONTROL
    // =======================

    @Operation(summary = "Start scraping process", description = "Starts asynchronous scraping task for the current user.")
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<String>> startScraping(@RequestBody ScraperRequest request) {
        try {
            String userId = jwtService.getPublicIdFromCurrentRequest();
            ResponseEntity<ApiResponse<String>> response = scraperService.startScraping(request);
//            actionHistoryServices.addActionHistory(userId, "[Scraper] Started scraping");
            log.info("🚀 Scraping started for user {}", userId);
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to start scraping", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to start scraping: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Stop scraping process", description = "Stops the currently running scraping task for the current user.")
    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<String>> stopScraping() {
        try {
            String userId = jwtService.getPublicIdFromCurrentRequest();
            ResponseEntity<ApiResponse<String>> response = scraperService.stopScraping();
            log.info("🛑 Scraping stop requested by user {}", userId);
            return response;
        } catch (Exception e) {
            log.error("❌ Failed to stop scraping", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to stop scraping: " + e.getMessage()));
        }
    }

    // =======================
    // REAL-TIME STREAMING
    // =======================

    @Operation(summary = "Real-time job stream", description = "Streams scraped jobs in real time for the logged-in user using Server-Sent Events (SSE).")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Job> streamJobs() {
        try {
            String userId = jwtService.getPublicIdFromCurrentRequest();
            log.info("🔗 SSE connection opened for user {}", userId);
            return sseService.getOrCreateSinkForUser(userId).asFlux();
        } catch (Exception e) {
            log.error("❌ Failed to start real-time job stream", e);
            return Flux.error(new RuntimeException("Failed to establish real-time job stream: " + e.getMessage()));
        }
    }

    @Operation(summary = "Real-time job stream by userId", description = "Streams jobs for a given userId (admin or diagnostic use).")
    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Job> streamScrapedJobs(@PathVariable String userId) {
        log.info("🔗 SSE connection opened for user {}", userId);
        return sseService.getOrCreateSinkForUser(userId).asFlux();
    }

    // =======================
    // SCRAPER REQUEST CRUD
    // =======================

    @Operation(summary = "Create new ScraperRequest", description = "Stores a new ScraperRequest configuration in the database.")
    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<ScraperRequest>> createScraperRequest(@RequestBody ScraperRequest request) {
        try {
            return scraperService.createScraperRequest(request);
        } catch (Exception e) {
            log.error("❌ Failed to create ScraperRequest", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to create ScraperRequest: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get all ScraperRequests (admin)", description = "Retrieves all saved ScraperRequest configurations. Requires ADMIN role.")
    @GetMapping("/admin/requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ScraperRequest>>> getAllScraperRequests() {
        try {
            return scraperService.getAllScraperRequests();
        } catch (Exception e) {
            log.error("❌ Failed to retrieve ScraperRequests", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to get ScraperRequests: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get ScraperRequests for logged-in user", description = "Retrieves ScraperRequests created by the current authenticated user.")
    @GetMapping("/requests/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ScraperRequest>>> getScraperRequestsByCurrentUser() {
        try {
            return scraperService.getScraperRequestsByCurrentUser();
        } catch (Exception e) {
            log.error("❌ Failed to retrieve user's ScraperRequests", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to get user's ScraperRequests: " + e.getMessage()));
        }
    }

    @Operation(summary = "Get ScraperRequest by Public ID", description = "Retrieves a single ScraperRequest by its Public UUID.")
    @GetMapping("/requests/{publicId}")
    public ResponseEntity<ApiResponse<ScraperRequest>> getScraperRequestByPublicId(@PathVariable UUID publicId) {
        try {
            return scraperService.getScraperRequestByPublicId(publicId);
        } catch (Exception e) {
            log.error("❌ Failed to retrieve ScraperRequest by ID", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to get ScraperRequest: " + e.getMessage()));
        }
    }

    @Operation(summary = "Update ScraperRequest", description = "Updates an existing ScraperRequest by Public ID.")
    @PutMapping("/requests/{publicId}")
    public ResponseEntity<ApiResponse<ScraperRequest>> updateScraperRequest(@PathVariable UUID publicId, @RequestBody ScraperRequest updatedRequest) {
        try {
            return scraperService.updateScraperRequest(publicId, updatedRequest);
        } catch (Exception e) {
            log.error("❌ Failed to update ScraperRequest", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to update ScraperRequest: " + e.getMessage()));
        }
    }

    @Operation(summary = "Delete ScraperRequest", description = "Deletes an existing ScraperRequest by Public ID.")
    @DeleteMapping("/requests/{publicId}")
    public ResponseEntity<ApiResponse<String>> deleteScraperRequest(@PathVariable UUID publicId) {
        try {
            return scraperService.deleteScraperRequest(publicId);
        } catch (Exception e) {
            log.error("❌ Failed to delete ScraperRequest", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to delete ScraperRequest: " + e.getMessage()));
        }
    }
}
