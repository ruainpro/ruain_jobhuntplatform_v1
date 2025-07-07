package com.dao.rjobhunt.Service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class JobService {

    @Value("${adzuna.app-id}")
    private String appId;

    @Value("${adzuna.app-key}")
    private String appKey;

    @Value("${adzuna.country}")
    private String country;

    @Value("${adzuna.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public JsonNode searchAdzunaJobs(String keyword, String where, String category, String sortBy, int page) {
        logConfigValues();

        String url = String.format("%s/%s/search/%d", baseUrl, country, page);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("app_id", appId)
                .queryParam("app_key", appKey)
                .queryParam("results_per_page", 20)
                .queryParam("what", keyword)   // ✅ flexible keyword search
                .queryParam("where", where)
                .queryParam("sort_by", sortBy);

        // ✅ Add category if provided
        if (category != null && !category.isBlank()) {
            builder.queryParam("category", category);
        }

        String finalUrl = builder.toUriString();
//        log.info("🔗 Requesting Adzuna API URL: {}", finalUrl);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                finalUrl,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                JsonNode.class
        );

        JsonNode responseBody = response.getBody();
//        log.info("✅ Adzuna API responded with status {} and {} results.",
//                response.getStatusCode(), responseBody != null ? responseBody.path("count").asInt(-1) : -1);

        return responseBody;
    }
    
    public JsonNode fetchAdzunaCategories() {
        String url = String.format("%s/%s/categories", baseUrl, country);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("app_id", appId)
                .queryParam("app_key", appKey);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
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
