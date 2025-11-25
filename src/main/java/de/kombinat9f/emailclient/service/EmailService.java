package de.kombinat9f.emailclient.service;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    private JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendOneEmail(Map<String, String> emailContent) {
        Map<String, String> emailContentParts = sanitizeData(emailContent);
        if (emailContentParts == null) {
            return false;
        } else if (emailContentParts.get("payloadUri") == null) {
            sendSimpleEmail(emailContentParts.get("emailAddress"), emailContentParts.get("subject"),
                    emailContentParts.get("message"));
        } else if (emailContentParts.get("payloadUri") != null) {
            // TODO send complex email with attachment
        }
        return true;

    }

    private Map<String, String> sanitizeData(Map<String, String> emailContent) {
        // Validate and normalize incoming data, return null on invalid input.
        String emailAddress = null;
        String subject = null;
        String message = null;
        String payloadUri = null;

        if (StringUtils.isNotEmpty(emailContent.get("to"))) {
            emailAddress = emailContent.get("to");

            if (!emailAddress.matches(EMAIL_PATTERN)) {
                logger.warn("Invalid format for Email-Address: " + emailAddress, new IllegalArgumentException());
                return null;
            }
        } else {
            logger.warn("Missing email address", new IllegalArgumentException());
            return null;
        }

        if (StringUtils.isNotEmpty(emailContent.get("subject"))) {
            subject = emailContent.get("subject");
        } else {
            logger.warn("Invalid format for subject for email to: " + emailAddress, new IllegalArgumentException());
            return null;
        }
        if (StringUtils.isNotEmpty(emailContent.get("message"))) {
            message = emailContent.get("message");
        } else {
            logger.warn("Invalid format for message for email to: " + emailAddress, new IllegalArgumentException());
            return null;
        }
        if (StringUtils.isNotEmpty(emailContent.get("payloadUri"))) {
            payloadUri = emailContent.get("payloadUri");
        }

        // Use a mutable map so we can store null values for payloadUri if absent.
        Map<String, String> result = new HashMap<>();
        result.put("emailAddress", emailAddress);
        result.put("subject", subject);
        result.put("message", message);
        result.put("payloadUri", payloadUri);

        return result;
    }

    private void sendSimpleEmail(String to, String subject, String message) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(to);
        email.setSubject(subject);
        email.setText(message);
        mailSender.send(email);
    }
}
