package de.kombinat9f.emailclient.service;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import de.kombinat9f.emailclient.controller.AttachmentClient;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

@Service
public class EmailService {

    @Value("${from.email}")
    private String from;

    Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    private JavaMailSender mailSender;
    private AttachmentClient restClient;

    public EmailService(JavaMailSender mailSender, AttachmentClient restClient) {
        this.mailSender = mailSender;
        this.restClient = restClient;
    }

    public void sendOneEmail(Map<String, String> emailContent) {
        Map<String, String> emailContentParts = sanitizeData(emailContent);
        if (emailContentParts == null) {
            logger.warn("Email data is faulty. There could be no email created", emailContent);
        } else if (emailContentParts.get("payloadUri") == null) {
            try {
                sendEmail(emailContentParts.get("emailAddress"), emailContentParts.get("subject"),
                        emailContentParts.get("message"));
            } catch (MailException ex) {
                logger.error("Sending email is not possible after retry. Check email provider.", ex);
            }
        } else if (emailContentParts.get("payloadUri") != null) {
            try {
                sendEmailWithAttachment(emailContentParts.get("emailAddress"), emailContentParts.get("subject"),
                        emailContentParts.get("message"), emailContentParts.get("payloadUri"), emailContentParts.get("dataType"));
            } catch (MailException ex) {
                logger.error("Sending email with attachment is not possible after retry. Check email provider.", ex);
            } catch (MessagingException mex) {
                logger.error("Creating email with attachment is not possible.", mex);
            } catch (URISyntaxException uex) {
                logger.error("Creating URI from payloadUri {} is not possible.", emailContentParts.get("payloadUri"),
                        uex);
            } catch (IOException ex) {
                logger.error("Downloading attachment for email to {} is not possible.", emailContentParts.get("to"), ex);
            }
        }
    }

    private Map<String, String> sanitizeData(Map<String, String> emailContent) {

        String emailAddress = null;
        String subject = null;
        String message = null;
        String payloadUri = null;
        String dataType = null;

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
            if (StringUtils.isNotEmpty(emailContent.get("dataType"))){
            payloadUri = emailContent.get("payloadUri");
            dataType = emailContent.get("dataType");
            }
        }
    

        // Use a mutable map so we can store null values for payloadUri if absent.
        Map<String, String> result = new HashMap<>();
        result.put("emailAddress", emailAddress);
        result.put("subject", subject);
        result.put("message", message);
        result.put("payloadUri", payloadUri);
        result.put("dataType", dataType);

        return result;
    }

    // annotation based retryable could not be unit tested sufficiently
    // maxRetries = 5 means up to 5 retries after the initial attempt -> total
    // attempts = 6
    private static final int MAX_RETRIES = 5;

    public void sendEmail(String to, String subject, String message) throws MailException {
        int attempts = 0;
        int maxAttempts = 1 + MAX_RETRIES;
        Optional<MailException> lastException = Optional.empty();

        if (from == null) {
            from = "default@origin.com";
        }
        while (attempts < maxAttempts) {
            try {
                SimpleMailMessage email = new SimpleMailMessage();
                email.setTo(to);
                email.setSubject(subject);
                email.setText(message);
                // without, smtp servers might reject the email
                email.setFrom(from);
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
        lastException.ifPresent(value -> {
            throw value;
        });
    }

    private void sendEmailWithAttachment(String to, String subject, String message, String payloadUri, String dataType)
            throws MessagingException, MailException, URISyntaxException, IOException {

        int attempts = 0;
        int maxAttempts = 1 + MAX_RETRIES;
        Optional<MailException> lastException = Optional.empty();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        URI sanitizedUri = new URI(payloadUri).normalize();

        if (from == null) {
            from = "default@origin.com";
        }

        // No retry for MessagingException
        while (attempts < maxAttempts) {
            try {
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(message);
                // without, smtp servers might reject the email
                helper.setFrom(from);
               
                // could throw IOException                
                byte[] file = restClient.prepareAttachmentFile(sanitizedUri);
                DataSource dataSource = new ByteArrayDataSource(file, dataType);
                helper.addAttachment("Attachment", dataSource);
                
                // helper.addAttachment("Invoice", file);
                mailSender.send(mimeMessage);
                return;
            } catch (MailException ex) {
                attempts++;
                lastException = Optional.of(ex);
                logger.warn("Attempt {} to send email failed", attempts, ex);
                // no sleep/backoff here to keep tests fast
            }
        }
        // rethrow the last exception after all attempts failed
        lastException.ifPresent(value -> {
            throw value;
        });
    }
}