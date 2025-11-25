package de.kombinat9f.emailclient.service;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
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

    public void sendOneEmail(Map<String, String> emailContent) {
        Map<String, String> emailContentParts = sanitizeData(emailContent);
        if (emailContentParts == null) {
            logger.warn("Email data is faulty. There could be no email created", emailContent);
        } else if (emailContentParts.get("payloadUri") == null) {
            try {
                sendSimpleEmail(emailContentParts.get("emailAddress"), emailContentParts.get("subject"),
                        emailContentParts.get("message"));
            } catch (MailException ex) {
                logger.error("Sending email is not possible after retry. Check email provider.", ex);
            }
        } else if (emailContentParts.get("payloadUri") != null) {
            // TODO send complex email with attachment
            // do we need mime type in post request?
        }
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

    // annotation based retryable could not be unit tested sufficiently
    // maxRetries = 5 means up to 5 retries after the initial attempt -> total attempts = 6
    private static final int MAX_RETRIES = 5;

    public void sendSimpleEmail(String to, String subject, String message) throws MailException {
        int attempts = 0;
        int maxAttempts = 1 + MAX_RETRIES;
        Optional<MailException> lastException = Optional.empty();

        while (attempts < maxAttempts) {
            try {
                SimpleMailMessage email = new SimpleMailMessage();
                email.setTo(to);
                email.setSubject(subject);
                email.setText(message);
                mailSender.send(email);
                // success
                return;
            } catch (MailException ex) {
                attempts++;
                lastException = Optional.of(ex);
                logger.warn("Attempt {} to send email failed", attempts, ex);
                // no sleep/backoff here to keep tests fast
            }
        }
        // rethrow the last exception after all attempts failed
        lastException.ifPresent(value -> { throw value; });
    }

    private void sendMailWithAttachment(String to, String subject, String message, String payloadUri) {

    }

}