package com.dao.rjobhunt.Service;

import com.dao.rjobhunt.models.Job;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SMSService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.from.number}")
    private String fromNumber;

    public void sendJobAlert(String toPhoneNumber, List<Job> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            log.warn("❌ No jobs found to send via SMS");
            return;
        }

        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            log.warn("❌ Destination phone number is missing");
            return;
        }

        if (accountSid == null || authToken == null || fromNumber == null) {
            log.error("❌ Twilio credentials are not set. Aborting SMS send.");
            return;
        }

        try {
            Twilio.init(accountSid, authToken);

            // Build a short summary of up to 3 jobs
            String summary = jobs.stream().limit(3)
                    .map(job -> String.format(
                            "%s @ %s (%s)",
                            job.getTitle(),
                            job.getCompany(),
                            job.getLocation()))
                    .collect(Collectors.joining("\n\n"));

            if (jobs.size() > 3) {
                summary += "\n\n...and more on RuAin JobHunt!";
            }

            String firstUrl = jobs.get(0).getUrl();
            if (firstUrl != null && !firstUrl.isBlank()) {
                summary += "\n\n🔗 First Job: " + firstUrl;
            }

            String messageBody = "✨ Job Matches Found!\n\n" + summary + "\n\n— RuAin JobHunt";

            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber.trim()),
                    new PhoneNumber(fromNumber),
                    messageBody
            ).create();

            log.info("✅ SMS sent to {}. SID: {}", maskNumber(toPhoneNumber), message.getSid());

        } catch (Exception e) {
            log.error("❌ Failed to send SMS to {}: {}", maskNumber(toPhoneNumber), e.getMessage(), e);
        }
    }

    private String maskNumber(String phone) {
        if (phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
}
