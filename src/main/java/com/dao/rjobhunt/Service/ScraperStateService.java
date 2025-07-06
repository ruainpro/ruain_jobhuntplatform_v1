package com.dao.rjobhunt.Service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScraperStateService {

    // Map scraper publicId -> stop flag (true = stop requested)
    private final Map<UUID, Boolean> stopFlags = new ConcurrentHashMap<>();

    // Called by /stop endpoint
    public void setStopped(UUID scraperRequestId, boolean stopped) {
        stopFlags.put(scraperRequestId, stopped);
    }

    // Called inside your scraping loop
    public boolean isStopped(UUID scraperRequestId) {
        return stopFlags.getOrDefault(scraperRequestId, false);
    }

    // Optional: clear flags after scraper finishes
    public void clear(UUID scraperRequestId) {
        stopFlags.remove(scraperRequestId);
    }
}