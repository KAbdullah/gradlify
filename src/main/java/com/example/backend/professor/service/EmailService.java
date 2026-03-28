/**
 * Service responsible for sending emails using Spring's JavaMailSender.
 * This service is primarily used to send registration and password reset.
 */

package com.example.backend.professor.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${gradify.mail.from:}")
    private String fromAddress;

    @Value("${gradify.mail.from.name:Gradify Support}")
    private String fromName;

    @Value("${gradify.mail.replyTo:}")
    private String replyTo;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    /**
     * Sends a simple plain-text email to a single recipient.
     *
     * @param toEmail the destination email address
     * @param subject the subject of the email
     * @param content the plain text body of the email
     */
    public void sendSimpleEmail(String toEmail, String subject, String content) {
        if (mailSender == null) {
            logger.warn("Email skipped for '{}': JavaMailSender is not configured.", toEmail);
            return;
        }

        if (fromAddress == null || fromAddress.isBlank()) {
            logger.warn("Email skipped for '{}': gradify.mail.from is not configured.", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            // Helper allows setting fields like recipient, subject, body, etc.
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, false);
            helper.setFrom(new InternetAddress(fromAddress, fromName));  // <-- important

            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
