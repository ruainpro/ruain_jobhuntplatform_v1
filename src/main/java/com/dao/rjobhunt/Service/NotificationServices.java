package com.dao.rjobhunt.Service;

import com.dao.rjobhunt.models.*;
import com.dao.rjobhunt.repository.UserInfoRepository;
import com.dao.rjobhunt.repository.UserNotificationFeedRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.*;

import com.dao.rjobhunt.models.*;
import com.dao.rjobhunt.repository.UserInfoRepository;
import com.dao.rjobhunt.repository.UserNotificationFeedRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationServices {

    @Autowired private UserInfoRepository userRepo;
    @Autowired private UserNotificationFeedRepository feedRepo;
    @Autowired private EmailService emailService;
    @Autowired private SMSService smsService;
    @Autowired private DiscordService discordService;
    @Autowired private NotificationSender notificationSender;

    /**
     * Notify all users with a list of jobs (batch email per user)
     */
    public void notifyTopUsersWithJobs(List<Job> jobs, String jobType, int limit) {
        if (jobs == null || jobs.isEmpty()) {
            log.warn("🚫 No jobs to notify.");
            return;
        }

        List<User> allUsers = userRepo.findAll();
        int notifyCount = 0;

        log.info("📬 Sending job alert to top {} users with {} jobs", limit, jobs.size());

        for (User user : allUsers) {
            if (notifyCount >= limit) break;

            Notification prefs = user.getNotification();
            if (prefs == null || (!prefs.isEmailEnabled() && !prefs.isSmsEnabled() && !prefs.isDiscordEnabled())) {
                log.warn("⚠️ Skipping user {} — no notification channels enabled.", user.getEmail());
                continue;
            }

            try {
                boolean notified = notificationSender.sendToUser(user.getPublicId(), jobs);

                if (notified) {
                    for (Job job : jobs) {
                        saveUserNotificationFeed(user.getPublicId(), job.getPublicId().toString(), jobType);
                    }
                    log.info("✅ Notified user: {}", user.getEmail());
                    notifyCount++;
                }

            } catch (Exception e) {
                log.error("❌ Error notifying user {}: {}", user.getEmail(), e.getMessage(), e);
            }
        }

        log.info("🏁 Finished notifying users. Total notified: {}", notifyCount);
    }

    /**
     * Avoid duplicate notification by requestId
     */
    public void sendNotificationsIfNotSent(UUID userId, String requestId, String jobSource, List<Job> jobs) {
        if (feedRepo.existsByUserIdAndRequestId(userId, requestId)) return;

        boolean notified = notificationSender.sendToUser(userId, jobs);

        if (notified) {
            for (Job job : jobs) {
                UserNotificationFeed feed = UserNotificationFeed.builder()
                        .userId(userId)
                        .requestId(requestId)
                        .jobSource(jobSource)
                        .jobId(job.getPublicId().toString())
                        .notifiedAt(LocalDateTime.now())
                        .build();
                feedRepo.save(feed);
            }
        }
    }

    /**
     * Save one feed entry
     */
    private void saveUserNotificationFeed(UUID userId, String jobId, String jobType) {
        UserNotificationFeed feed = UserNotificationFeed.builder()
                .userId(userId)
                .jobId(jobId)
                .jobSource(jobType)
                .notifiedAt(LocalDateTime.now())
                .build();
        feedRepo.save(feed);
    }

    /**
     * 🔁 Daily job — example with mock jobs
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void dailyNotificationJob() {
        log.info("🔁 Running daily scheduled job...");

        List<Job> mockJobs = List.of(
            Job.builder().title("Java Developer").company("Amazon").location("Toronto").url("https://example.com/java").publicId(UUID.randomUUID()).description("Build microservices.").build(),
            Job.builder().title("React Developer").company("Shopify").location("Remote").url("https://example.com/react").publicId(UUID.randomUUID()).description("Modern frontend development.").build()
        );

        notifyTopUsersWithJobs(mockJobs, "daily-simjob", 50);
    }

    /**
     * 📤 Test method to send to one user
     */
    public void runTestNotification(UUID userId, List<Job> jobList) {
        Optional<User> userOpt = userRepo.findByPublicId(userId);
        if (userOpt.isEmpty() || jobList == null || jobList.isEmpty()) return;

        User user = userOpt.get();
        Notification prefs = user.getNotification();
        boolean notified = false;

        try {
            if (prefs.isEmailEnabled()) {
                emailService.sendJobAlertEmail(user.getEmail(), jobList);
                notified = true;
            }
            if (prefs.isSmsEnabled() && prefs.getPhoneNumber() != null) {
                // smsService.sendJobAlert(prefs.getPhoneNumber(), jobList);
                notified = true;
            }
            if (prefs.isDiscordEnabled() && prefs.getDiscordWebhook() != null) {
                // discordService.sendJobAlert(prefs.getDiscordWebhook(), jobList);
                notified = true;
            }

            if (notified) {
                for (Job job : jobList) {
                    saveUserNotificationFeed(user.getPublicId(), job.getPublicId().toString(), "manual-test");
                }
                log.info("✅ Test notification sent to {}", user.getEmail());
            }

        } catch (Exception e) {
            log.error("❌ Failed test notification for {}: {}", user.getEmail(), e.getMessage());
        }
    }
    
}
