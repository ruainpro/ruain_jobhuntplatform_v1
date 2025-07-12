package com.dao.rjobhunt.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.dao.rjobhunt.models.Job;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Autowired
    private TemplateEngine templateEngine;

    
    /**
     * Send an HTML email using the given template and dynamic placeholders.
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom(senderEmail); // Optional: set custom from address here

        mailSender.send(message);
    }
    
    
    
    public void sendJobAlertEmail(String toEmail, List<Job> jobList) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, 
            MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
            StandardCharsets.UTF_8.name());

        Context context = new Context();
        context.setVariable("USER_NAME", toEmail);
        context.setVariable("JOBS", jobList);
        context.setVariable("SENT_DATE", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));

        String html = templateEngine.process("email/jobemail", context);

        helper.setTo(toEmail);
        helper.setSubject("📬 New Jobs Just for You - RuAin");
        helper.setText(html, true);
        helper.setFrom("ruainjobhunt@gmail.com");

        mailSender.send(message);
    }


    /**
     * Load HTML template from classpath (e.g., resources/templates).
     */
    private String loadTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
    }
}
