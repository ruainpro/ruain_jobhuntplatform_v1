package com.dao.rjobhunt.Service;

import com.dao.rjobhunt.models.Job;
import com.dao.rjobhunt.models.Notification;
import com.dao.rjobhunt.models.User;
import com.dao.rjobhunt.repository.UserInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationSender {

    @Autowired private EmailService emailService;
    @Autowired private SMSService smsService;
    @Autowired private DiscordService discordService;
    @Autowired private UserInfoRepository userRepo;

    public boolean sendToUser(UUID userId, List<Job> jobs) {
        Optional<User> userOpt = userRepo.findByPublicId(userId);
        if (userOpt.isEmpty() || jobs == null || jobs.isEmpty()) return false;

        User user = userOpt.get();
        Notification prefs = user.getNotification();
        if (prefs == null) return false;

        boolean notified = false;

        try {
            if (prefs.isEmailEnabled()) {
                emailService.sendJobAlertEmail(user.getEmail(), jobs); 
                notified = true;
            }

            if (prefs.isSmsEnabled() && prefs.getPhoneNumber() != null) {
                 smsService.sendJobAlert(prefs.getPhoneNumber(), jobs);
                notified = true;
            }

            if (prefs.isDiscordEnabled() && prefs.getDiscordWebhook() != null) {
                 discordService.sendJobAlert(prefs.getDiscordWebhook(), jobs);
                notified = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return notified;
    }

}