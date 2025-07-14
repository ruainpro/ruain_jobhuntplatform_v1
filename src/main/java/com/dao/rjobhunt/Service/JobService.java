package com.dao.rjobhunt.Service;

import com.dao.rjobhunt.models.Job;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    @Value("${adzuna.app-id}")
    private String appId;

    @Value("${adzuna.app-key}")
    private String appKey;

    @Value("${adzuna.country}")
    private String country;

    @Value("${adzuna.base-url}")
    private String baseUrl;

    private final NotificationServices notificationServices;

    private final RestTemplate restTemplate = new RestTemplate();

    public JsonNode searchAdzunaJobs(String keyword, String where, String category, String sortBy, int page) {
        return searchAdzunaJobs(keyword, where, category, sortBy, page, false, null);
    }

    public JsonNode searchAdzunaJobs(String keyword, String where, String category, String sortBy, int page, boolean notify, String userId) {
        logConfigValues();

        String url = String.format("%s/%s/search/%d", baseUrl, country, page);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("app_id", appId)
                .queryParam("app_key", appKey)
                .queryParam("results_per_page", 20)
                .queryParam("what", keyword)
                .queryParam("where", where)
                .queryParam("sort_by", sortBy);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/122.0.0.0 Safari/537.36");
        HttpEntity<String> entity = new HttpEntity<>(headers);


        if (category != null && !category.isBlank()) {
            builder.queryParam("category", category);
        }

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                JsonNode.class
        );

        JsonNode jobs = response.getBody();

        // ✅ Notify logic (if enabled)
        if (notify && page == 1 && jobs != null && jobs.has("results")) {
            JsonNode results = jobs.get("results");
            List<Job> jobList = new ArrayList<>();

            int count = 0;
            for (JsonNode jobNode : results) {
                if (count++ >= 50) break;

                try {
                    Job job = Job.builder()
                            .title(jobNode.hasNonNull("title") ? jobNode.get("title").asText() : "No Title")
                            .company(jobNode.has("company") && jobNode.get("company").has("display_name")
                                    ? jobNode.get("company").get("display_name").asText()
                                    : "Unknown")
                            .location(jobNode.has("location") && jobNode.get("location").has("display_name")
                                    ? jobNode.get("location").get("display_name").asText()
                                    : "Unknown")
                            .description(jobNode.has("description") ? jobNode.get("description").asText() : "")
                            .url(jobNode.has("redirect_url") ? jobNode.get("redirect_url").asText() : "")
                            .publicId(UUID.randomUUID())
                            .build();

                    jobList.add(job);

                } catch (Exception e) {
                    log.error("❌ Error parsing job from Adzuna: {}", e.getMessage(), e);
                }
            }

            // ✅ Now notify the user with batch of jobs
            if (!jobList.isEmpty()) {
                try {
                    notificationServices.sendNotificationsIfNotSent(
                            UUID.fromString(userId),
                            "scrapjob-notify-" + UUID.randomUUID(),
                            "scrapjob",
                            jobList
                    );
                } catch (Exception ex) {
                    log.error("❌ Failed to notify user {}: {}", userId, ex.getMessage(), ex);
                }
            }
        }

        return jobs;
    }

//    public JsonNode fetchAdzunaCategories() {
//        String url = String.format("%s/%s/categories", baseUrl, country);
//
//        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
//                .queryParam("app_id", appId)
//                .queryParam("app_key", appKey);
//
//        ResponseEntity<JsonNode> response = restTemplate.exchange(
//                builder.toUriString(),
//                HttpMethod.GET,
//                new HttpEntity<>(new HttpHeaders()),
//                JsonNode.class
//        );
//
//        return response.getBody();
//    }
    
    public JsonNode fetchAdzunaCategories() {
        String url = String.format("%s/%s/categories", baseUrl, country);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("app_id", appId)
                .queryParam("app_key", appKey);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("User-Agent", "Mozilla/5.0"); // Mimics browser

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                JsonNode.class
        );

        return response.getBody();
    }

    private void logConfigValues() {
        System.out.println("[DEBUG] Adzuna config: baseUrl=" + baseUrl +
                ", country=" + country +
                ", appId=" + appId +
                ", appKey=" + appKey);
    }
}
