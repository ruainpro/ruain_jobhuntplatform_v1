package com.dao.rjobhunt;


import com.dao.rjobhunt.Service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@SpringBootTest
public class NotificationEmailServiceTest {

    @Autowired
    private EmailService emailService;

//    @Test
//    public void testSendJobAlertEmail() {
//        Map<String, Object> vars = Map.of(
//            "USER_NAME", "Rupesh",
//            "JOB_TITLE", "Lead Full Stack Developer",
//            "COMPANY_NAME", "RuAin Labs",
//            "LOCATION", "Toronto / Hybrid",
//            "SALARY", "$110,000 – $130,000 CAD",
//            "DESCRIPTION", "Spearhead development for intelligent job-matching software using Spring Boot, React, and scalable cloud microservices.",
//            "JOB_URL", "https://yourdomain.com/jobs/lead-fullstack",
//            "SENT_DATE", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
//        );
//
//        try {
//            emailService.sendJobAlertEmail("ruainpro@gmail.com", vars);  // <-- Change this to your test email
//            System.out.println("✅ Job alert email sent successfully.");
//        } catch (Exception e) {
//            e.printStackTrace();
//            assert false : "❌ Failed to send job alert email";
//        }
//    }
}