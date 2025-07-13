package com.dao.rjobhunt.Service;

import org.springframework.stereotype.Service;

import com.dao.rjobhunt.models.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@Slf4j
@Service
public class DiscordService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendJobAlert(String webhookUrl, List<Job> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            log.warn("No jobs to send to Discord");
            return;
        }

        StringBuilder content = new StringBuilder("📢 **New Job Matches from RuAin JobHunt!**\n\n");
        for (int i = 0; i < Math.min(jobs.size(), 5); i++) {
            Job job = jobs.get(i);
            content.append("**").append(job.getTitle()).append("** at *")
                   .append(job.getCompany()).append("* — `")
                   .append(job.getLocation()).append("`\n");

            if (job.getUrl() != null && !job.getUrl().isEmpty()) {
                content.append("🔗 [Apply Now](").append(job.getUrl()).append(")\n");
            }
            content.append("\n");
        }

        if (jobs.size() > 5) {
            content.append("...and more jobs available in your dashboard.");
        }

        // Prepare JSON payload with headers
        var payload = new java.util.HashMap<String, String>();
        payload.put("content", content.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);  // 🔥 Required by Discord

        HttpEntity<Object> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(webhookUrl, request, String.class);
            log.info("✅ Discord job alert sent to webhook.");
        } catch (Exception e) {
            log.error("❌ Failed to send Discord job alert: {}", e.getMessage(), e);
        }
    }
}