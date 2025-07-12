package com.dao.rjobhunt.Controller.authentication;


import com.dao.rjobhunt.Security.JwtService;
import com.dao.rjobhunt.Service.ActionHistoryServices;
import com.dao.rjobhunt.Service.JobService;
import com.dao.rjobhunt.Service.NotificationServices;
import com.dao.rjobhunt.dto.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Job Search", description = "Fetch real-time live jobs from Adzuna")
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ActionHistoryServices actionHistoryServices;
    
    @Autowired
    private NotificationServices notificationServices;

    @Operation(summary = "Search live jobs", description = "Fetch live jobs with optional keyword, location, category, sort order, and pagination. Optionally send notifications for top results.")
    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<JsonNode>> getLiveJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "Canada") String where,
            @RequestParam(defaultValue = "date") String sort_by,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "false") boolean notify)
  {
        String userId = jwtService.getPublicIdFromCurrentRequest();

        String effectiveKeyword = (keyword == null || keyword.trim().isEmpty()) ? "Software Engineer" : keyword;

        actionHistoryServices.addActionHistory(userId,
                String.format("Searched jobs with keyword='%s', where='%s', category='%s', sort_by='%s', page=%d, notify=%b",
                        effectiveKeyword, where, category != null ? category : "N/A", sort_by, page, notify));

//        JsonNode jobs = jobService.searchAdzunaJobs(effectiveKeyword, where, category, sort_by, page);
        JsonNode jobs = jobService.searchAdzunaJobs(effectiveKeyword, where, category, sort_by, page, notify, userId);


//        if (notify && page == 1 && jobs.has("results")) {
//            jobService.notifyTopJobsToUser(jobs.get("results"), userId);
//        }

        return ResponseEntity.ok(ApiResponse.success("Fetched live jobs successfully", jobs));
    }
    
    @Operation(summary = "Search live jobs", description = "Fetch Jobs categorues")
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<JsonNode>> getCategories() {
        JsonNode categories = jobService.fetchAdzunaCategories();
        return ResponseEntity.ok(ApiResponse.success("Fetched categories successfully", categories));
    }

}