package com.dao.rjobhunt.Service;

import com.dao.rjobhunt.Security.JwtService;
import com.dao.rjobhunt.dto.ApiResponse;
import com.dao.rjobhunt.models.Job;
import com.dao.rjobhunt.models.ParserType;
import com.dao.rjobhunt.models.Platform;
import com.dao.rjobhunt.models.ScraperRequest;
import com.dao.rjobhunt.repository.PlatformRepository;
import com.dao.rjobhunt.repository.ScraperRequestRepository;
import com.mongodb.client.result.DeleteResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperService {

    private final PlatformRepository platformRepo;
    private final ScraperRequestRepository scraperRequestRepo;
    private final IndeedScraperService indeedScraperService;

//    private final JwtService jwtService;
//    private final ActionHistoryServices actionHistoryServices;
//    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private ActionHistoryServices actionHistoryServices;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, Future<?>> activeTasks = new ConcurrentHashMap<>();
    private List<Job> lastScrapedJobs = new ArrayList<>();
    
    @Autowired
    private MongoTemplate mongoTemplate;

    // =======================
    // SCRAPING CONTROL
    // =======================

    public ResponseEntity<ApiResponse<String>> startScraping(ScraperRequest request) {
        try {
            validateRequest(request);

            String userId = jwtService.getPublicIdFromCurrentRequest();
            request.setUserId(UUID.fromString(userId));

            UUID platformUuid = UUID.fromString(request.getPlatformId());
            Platform platform = platformRepo.findByPublicId(platformUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Platform not found with ID: " + request.getPlatformId()));

            
            // Stop any existing task first
            stopScraping(userId);

            Future<?> task = executorService.submit(() -> {
                try {
                    log.info("🚀 [Scraper] Started async scraping for user {}", userId);
                    actionHistoryServices.addActionHistory(userId, "[Scraper] Started async scraping task");

                    List<Job> scrapedJobs = doScraping(request, platform, userId);

                    lastScrapedJobs = scrapedJobs;
                    actionHistoryServices.addActionHistory(userId, "[Scraper] Finished scraping with " + scrapedJobs.size() + " jobs");
                    log.info("✅ [Scraper] Finished scraping {} jobs for user {}", scrapedJobs.size(), userId);

                } catch (InterruptedException ex) {
                    log.warn("🛑 Scraping task interrupted for user {}", userId);
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    log.error("❌ Error during async scraping for user {}: {}", userId, ex.getMessage(), ex);
                } finally {
                    activeTasks.remove(userId);
                }
            });

            activeTasks.put(userId, task);
            
            // ✅ Save the ScraperRequest here automatically
            createScraperRequest(request);

            return ResponseEntity.ok(ApiResponse.success("Async scraping started for user " + userId, null));
        } catch (Exception e) {
            log.error("❌ Error during startScraping: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to start scraping: " + e.getMessage()));
        }
    }

    public ResponseEntity<ApiResponse<String>> stopScraping() {
        try {
            String userId = jwtService.getPublicIdFromCurrentRequest();
            stopScraping(userId);
            return ResponseEntity.ok(ApiResponse.success("Scraping stop requested", null));
        } catch (Exception e) {
            log.error("❌ Failed to stop scraping", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to stop scraping: " + e.getMessage()));
        }
    }

    private void stopScraping(String userId) {
        Future<?> task = activeTasks.remove(userId);
        if (task != null && !task.isDone()) {
            task.cancel(true);
            actionHistoryServices.addActionHistory(userId, "[Scraper] Stopped scraping task");
            log.info("🛑 Scraping task cancelled for user {}", userId);
        } else {
            log.info("⚠️ No active scraping task to stop for user {}", userId);
        }
    }

    private List<Job> doScraping(ScraperRequest request, Platform platform, String userId) throws Exception {
        List<Job> allJobs = new ArrayList<>();
        int maxPages = request.getMaxPages() != null ? request.getMaxPages() : 5;

        for (int page = 1; page <= maxPages; page++) {
            if (Thread.currentThread().isInterrupted()) {
                log.info("🛑 Scraping interrupted at page {} for user {}", page, userId);
                throw new InterruptedException("Scraping manually stopped");
            }

            String platformName = platform.getName().toLowerCase();

            List<Job> jobsForPage;

            if (platformName.startsWith("inde")) {
                if (platform.getParserType().equals(ParserType.API)) {
                    jobsForPage = indeedScraperService.scrapeIndeed(
                            request.getQuery(),
                            request.getLocation(),
                            1,
                            platform.getApiKey(),
                            request.getUserId().toString()
                    );
                } else {
                    throw new UnsupportedOperationException("Only API parser supported currently for Indeed.");
                }
            } else if (platformName.startsWith("link")) {
                throw new UnsupportedOperationException("LinkedIn scraping not yet implemented");
            } else if (platformName.startsWith("custom")) {
                throw new UnsupportedOperationException("Custom platform scraping not yet implemented");
            } else {
                throw new IllegalArgumentException("Unsupported platform name: " + platform.getName());
            }
            
            allJobs.addAll(jobsForPage);
            log.info("📥 Scraped {} jobs on page {}/{}", jobsForPage.size(), page, maxPages);

            // Optional: artificial delay between pages for demonstration
            Thread.sleep(2000);
        }

        return allJobs;
    }

    // =======================
    // CRUD METHODS
    // =======================

    public ResponseEntity<ApiResponse<ScraperRequest>> createScraperRequest(ScraperRequest request) {
        try {
            String userId = jwtService.getPublicIdFromCurrentRequest();
            UUID userUuid = UUID.fromString(userId);

            // Populate required fields
            request.setPublicId(UUID.randomUUID());
            request.setUserId(userUuid);
            request.setCreatedDate(new Date());

            ScraperRequest saved = scraperRequestRepo.save(request);

            actionHistoryServices.addActionHistory(
                userId,
                "[Scraper] Created ScraperRequest with PublicId: " + saved.getPublicId()
            );

            return ResponseEntity.ok(ApiResponse.success("ScraperRequest created successfully", saved));
        } catch (Exception e) {
            log.error("❌ Failed to create ScraperRequest", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to create ScraperRequest: " + e.getMessage()));
        }
    }

    public ResponseEntity<ApiResponse<List<ScraperRequest>>> getAllScraperRequests() {
        try {
            List<ScraperRequest> all = scraperRequestRepo.findAll();
            return ResponseEntity.ok(ApiResponse.success("All ScraperRequests retrieved", all));
        } catch (Exception e) {
            log.error("❌ Failed to retrieve ScraperRequests", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to get ScraperRequests: " + e.getMessage()));
        }
    }

    public ResponseEntity<ApiResponse<List<ScraperRequest>>> getScraperRequestsByCurrentUser() {
        try {
            String userId = jwtService.getPublicIdFromCurrentRequest();
            UUID userUuid = UUID.fromString(userId);
            List<ScraperRequest> userRequests = scraperRequestRepo.findByUserId(userUuid);
            actionHistoryServices.addActionHistory(
                userId,
                "[Scraper] Retrieved ScraperRequests for logged-in user"
            );
            return ResponseEntity.ok(ApiResponse.success("ScraperRequests retrieved for user", userRequests));
        } catch (Exception e) {
            log.error("❌ Failed to retrieve user's ScraperRequests", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to get user's ScraperRequests: " + e.getMessage()));
        }
    }

    public ResponseEntity<ApiResponse<ScraperRequest>> getScraperRequestByPublicId(UUID publicId) {
        try {
            String userId = jwtService.getPublicIdFromCurrentRequest();
            return scraperRequestRepo.findByPublicId(publicId)
                    .map(req -> {
                        actionHistoryServices.addActionHistory(
                            userId,
                            "[Scraper] Retrieved ScraperRequest with PublicId: " + publicId
                        );
                        return ResponseEntity.ok(ApiResponse.success("ScraperRequest found", req));
                    })
                    .orElse(ResponseEntity.badRequest().body(ApiResponse.error("ScraperRequest not found with Public ID: " + publicId)));
        } catch (Exception e) {
            log.error("❌ Failed to retrieve ScraperRequest", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to get ScraperRequest: " + e.getMessage()));
        }
    }

    public ResponseEntity<ApiResponse<ScraperRequest>> updateScraperRequest(UUID publicId, ScraperRequest updatedRequest) {
        try {
            String userId = jwtService.getPublicIdFromCurrentRequest();
            updatedRequest.setUserId(UUID.fromString(userId));
            return scraperRequestRepo.findByPublicId(publicId)
                    .map(existing -> {
                        applyDynamicUpdate(existing, updatedRequest);
                        ScraperRequest saved = scraperRequestRepo.save(existing);
                        actionHistoryServices.addActionHistory(
                            userId,
                            "[Scraper] Updated ScraperRequest with PublicId: " + saved.getPublicId()
                        );
                        return ResponseEntity.ok(ApiResponse.success("ScraperRequest updated successfully", saved));
                    })
                    .orElse(ResponseEntity.badRequest().body(ApiResponse.error("ScraperRequest not found with Public ID: " + publicId)));
        } catch (Exception e) {
            log.error("❌ Failed to update ScraperRequest", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to update ScraperRequest: " + e.getMessage()));
        }
    }
    
    public ResponseEntity<ApiResponse<String>> deleteScraperRequestsByCurrentUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("No authenticated user found");
        }

        UUID userUuid = UUID.fromString(userId);

        // Build query matching the Binary UUID
        Query query = Query.query(Criteria.where("userId").is(userUuid));

        // Execute deletion and capture the result
        DeleteResult result = mongoTemplate.remove(query, ScraperRequest.class);

        long deletedCount = result.getDeletedCount();

        return ResponseEntity.ok(
            ApiResponse.success("Deleted " + deletedCount + " scraper request(s) for current user", null)
        );
    }
    
    public ResponseEntity<ApiResponse<String>> deleteScraperRequest(UUID publicId) {
        try {
            String userId = jwtService.getPublicIdFromCurrentRequest();
            return scraperRequestRepo.findByPublicId(publicId)
                    .map(req -> {
                        scraperRequestRepo.delete(req);
                        actionHistoryServices.addActionHistory(
                            userId,
                            "[Scraper] Deleted ScraperRequest with PublicId: " + publicId
                        );
                        return ResponseEntity.ok(ApiResponse.success("ScraperRequest deleted successfully", (String) null));
                    })
                    .orElse(ResponseEntity.badRequest().body(ApiResponse.error("ScraperRequest not found with Public ID: " + publicId)));
        } catch (Exception e) {
            log.error("❌ Failed to delete ScraperRequest", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to delete ScraperRequest: " + e.getMessage()));
        }
    }

    // =======================
    // VALIDATION
    // =======================

    private void validateRequest(ScraperRequest req) {
        if (req.getQuery() == null || req.getQuery().isBlank() || req.getQuery().length() < 2 || req.getQuery().length() > 255) {
            throw new IllegalArgumentException("Query must be 2-255 characters");
        }
        if (req.getLocation() == null || req.getLocation().isBlank() || req.getLocation().length() < 2 || req.getLocation().length() > 255) {
            throw new IllegalArgumentException("Location must be 2-255 characters");
        }
        if (req.getMaxPages() != null && (req.getMaxPages() < 1 || req.getMaxPages() > 50)) {
            throw new IllegalArgumentException("maxPages must be between 1 and 50");
        }
    }

    public static void applyDynamicUpdate(ScraperRequest existing, ScraperRequest updated) {
        if (updated.getQuery() != null) existing.setQuery(updated.getQuery());
        if (updated.getLocation() != null) existing.setLocation(updated.getLocation());
        if (updated.getMaxPages() != null) existing.setMaxPages(updated.getMaxPages());
        if (updated.getProfileKeywords() != null) existing.setProfileKeywords(updated.getProfileKeywords());
        if (updated.getTags() != null) existing.setTags(updated.getTags());
        if (updated.getCustomOptions() != null) existing.setCustomOptions(updated.getCustomOptions());
        if (updated.getEnableAutoLearning() != null) existing.setEnableAutoLearning(updated.getEnableAutoLearning());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getCreatedDate() != null) existing.setCreatedDate(updated.getCreatedDate());
        if (updated.getSource() != null) existing.setSource(updated.getSource());
        if (updated.getUrl() != null) existing.setUrl(updated.getUrl());
    }
}