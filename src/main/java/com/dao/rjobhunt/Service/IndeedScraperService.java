package com.dao.rjobhunt.Service;

import com.dao.rjobhunt.models.Job;
import com.dao.rjobhunt.webscrapper.UserAgentProvider;
import com.fasterxml.jackson.databind.*;
import okhttp3.*;
import java.util.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.regex.*;


@Service
public class IndeedScraperService {

    private static final Logger log = LoggerFactory.getLogger(IndeedScraperService.class);

    @Autowired
    private OkHttpClient client;
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Autowired
    private SseService sseService;

//	@Autowired
//	private JwtService jwtService;
//	
    public IndeedScraperService(OkHttpClient client) {
        this.client = client;
    }

    public List<Job> scrapeIndeed(String query, String location, int maxPages, String apiKey, String userId) throws Exception {
        List<Job> allJobs = new ArrayList<>();
        int page = 0;
        boolean keepGoing = true;

        while (keepGoing) {
        	
        	
            int start = page * 10;
            String targetUrl = "https://ca.indeed.com/jobs?q=" +
                    java.net.URLEncoder.encode(query + " " + location, StandardCharsets.UTF_8) +
                    "&start=" + start;

            String scraperApiUrl = "http://api.scraperapi.com?api_key=" + apiKey +
                    "&url=" + java.net.URLEncoder.encode(targetUrl, StandardCharsets.UTF_8);

            Request request = new Request.Builder()
                    .url(scraperApiUrl)
                    .header("User-Agent", UserAgentProvider.getRandomUserAgent())
                    .build();

            log.info("\n🔎 Scraping page {}: query='{}', location='{}'", page + 1, query, location);

            int maxRetries = 3;
            int attempt = 0;
            boolean success = false;

            while (attempt < maxRetries && !success) {
                attempt++;
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        int code = response.code();
                        String errorBody = response.body() != null ? response.body().string() : "No response body";
                        log.warn("⚠️ HTTP error on page {}, attempt {}/{}: {} - {}", page + 1, attempt, maxRetries, code, errorBody);

                        if (code >= 500 && code < 600 && attempt < maxRetries) {
                            int sleepTime = (int) Math.pow(2, attempt) * 1000;
                            log.info("🔄 Retrying after {} ms...", sleepTime);
                            Thread.sleep(sleepTime);
                            continue;
                        } else {
                            break; // Don't retry on 4xx
                        }
                    }

                    String html = response.body().string();
                    log.info("✅ Received page {} on attempt {} ({} bytes)", page + 1, attempt, html.length());

                    Pattern pattern = Pattern.compile("window\\.mosaic\\.providerData\\[\"mosaic-provider-jobcards\"\\]=(\\{.*?\\});");
                    Matcher matcher = pattern.matcher(html);

                    if (matcher.find()) {
                        String jsonBlob = matcher.group(1);
                        JsonNode root = mapper.readTree(jsonBlob);
                        JsonNode jobs = root.path("metaData").path("mosaicProviderJobCardsModel").path("results");

                        if (jobs.isArray() && jobs.size() > 0) {
                            for (JsonNode jobNode : jobs) {
                                Job job = mapJsonToJob(jobNode);
                                if (job != null) {
                                    allJobs.add(job);
                                    
//                                    String userId = jwtService.getPublicIdFromCurrentRequest();
                                    sseService.pushJobToClient(job, userId);
                                    
                                    log.info("\n📌 Job {}: {} @ {} ({}) | Salary: {}",
                                            allJobs.size(),
                                            job.getTitle(),
                                            job.getCompany(),
                                            job.getLocation(),
                                            job.getSalary());
                                    log.info("   ➔ Link: {}", job.getUrl());
                                }
                                Thread.sleep(1000 + new Random().nextInt(1000)); // polite + random delay
                            }
                            success = true;
                        } else {
                            log.info("🚨 No jobs found on page {}. Stopping.", page + 1);
                            keepGoing = false;
                            success = true;
                        }
                    } else {
                        log.error("❌ JSON blob not found on page {}. Stopping.", page + 1);
                        keepGoing = false;
                        success = true;
                    }
                } catch (Exception e) {
                    log.error("❗ Exception scraping page {}, attempt {}/{}: {}", page + 1, attempt, maxRetries, e.getMessage());
                    if (attempt < maxRetries) {
                        int sleepTime = (int) Math.pow(2, attempt) * 1000;
                        log.info("🔄 Retrying after {} ms...", sleepTime);
                        Thread.sleep(sleepTime);
                    }
                }
            }

            if (!success) {
                log.error("❌ Failed to scrape page {} after {} attempts. Stopping.", page + 1, maxRetries);
                break;
            }

            page++;
            if (page >= maxPages) {
                log.info("✅ Reached max pages limit ({}). Stopping.", maxPages);
                break;
            }

            Thread.sleep(1500); // polite delay between pages
        }

        return allJobs;
    }

    private Job mapJsonToJob(JsonNode job) {
        try {
            String jobKey = job.path("jobkey").asText();
            if (jobKey == null || jobKey.isBlank()) {
                log.error("❌ Missing or invalid jobkey; skipping job. Full job JSON: {}", job.toPrettyString());
                return null; // skip bad job
            }

            byte[] jobKeyBytes = jobKey.getBytes(StandardCharsets.UTF_8);
            if (jobKeyBytes.length == 0) {
                log.error("❌ jobKey bytes empty; skipping job: {}", job.toPrettyString());
                return null;
            }

            UUID jobUUID = UUID.nameUUIDFromBytes(jobKeyBytes);

            String title = job.path("title").asText("N/A");
            String company = job.path("company").asText("N/A");
            String loc = job.path("formattedLocation").asText("N/A");
            String salaryText = job.path("salarySnippet").path("text").asText(null);

            String snippet = job.path("snippet").asText("");
            String cleanSnippet = snippet
                    .replaceAll("(?i)<li[^>]*>", "\n• ")
                    .replaceAll("(?i)</li>", "")
                    .replaceAll("(?i)<[^>]+>", "")
                    .replaceAll("&amp;", "&")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .replaceAll("&quot;", "\"")
                    .replaceAll("&#39;", "'")
                    .trim();

            String jobLink = "https://ca.indeed.com/m/basecamp/viewjob?viewtype=embedded&jk=" + jobKey;

            return Job.builder()
                    .publicId(jobUUID)
                    .title(title)
                    .company(company)
                    .location(loc)
                    .salary(salaryText != null ? salaryText : "N/A")
                    .description(cleanSnippet.isEmpty() ? "N/A" : cleanSnippet)
                    .url(jobLink)
                    .scrapedDate(new Date())
                    .build();
        } catch (Exception e) {
            log.error("⚠️ Failed to parse job: {}", e.getMessage());
            return null;
        }
    }
}