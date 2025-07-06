package com.dao.rjobhunt.Service;

import com.dao.rjobhunt.models.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {

    // 🔹 Maintain a sink for each user's SSE connection (keyed by userId)
    private final Map<String, Sinks.Many<Job>> userStreams = new ConcurrentHashMap<>();

    /**
     * Get an existing sink for a user or create a new one if it doesn't exist.
     * This allows each user to have their own SSE stream for real-time job updates.
     *
     * @param userId the user's publicId
     * @return a Reactor Flux sink emitting Job events
     */
    public Sinks.Many<Job> getOrCreateSinkForUser(String userId) {
        return userStreams.computeIfAbsent(userId, key -> {
            log.info("📡 Creating new SSE stream for user: {}", userId);
            return Sinks.many().multicast().onBackpressureBuffer();
        });
    }

    /**
     * Push a scraped job event to the user's SSE stream.
     * If the user has an active SSE connection, they will receive this job instantly.
     *
     * @param job    the scraped job to send
     * @param userId the user's publicId
     */
    public void pushJobToClient(Job job, String userId) {
        Sinks.Many<Job> sink = userStreams.get(userId);
        if (sink != null) {
            sink.tryEmitNext(job);
            log.debug("✅ Sent job '{}' to user {}", job.getTitle(), userId);
        } else {
            log.warn("⚠️ No active SSE stream found for user {}; skipping job push.", userId);
        }
    }

    /**
     * Close and remove the sink for a user when they disconnect,
     * to avoid resource leaks.
     *
     * @param userId the user's publicId
     */
    public void closeSinkForUser(String userId) {
        Sinks.Many<Job> sink = userStreams.remove(userId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.info("🛑 Closed SSE stream for user {}", userId);
        }
    }
}