package de.kombinat9f.emailclient.service;

import java.util.Optional;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import de.kombinat9f.emailclient.controller.AttachmentClient;
import de.kombinat9f.emailclient.domain.EmailRequest;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;

@Service
public class EmailSenderService {

    @Value("${from.email}")
    private String from;

    Logger logger = LoggerFactory.getLogger(EmailSenderService.class);


    private JavaMailSender mailSender;
    private AttachmentClient restClient;

    public EmailSenderService(JavaMailSender mailSender, AttachmentClient restClient) {
        this.mailSender = mailSender;
        this.restClient = restClient;
    }

    public void sendOneEmail(EmailRequest emailRequest) {
        
        if (emailRequest == null) {
            logger.warn("Email data is faulty. There could be no email created", emailRequest);
        } else if (emailRequest.getPayloadUri() == null) {
            try {
                sendEmail(emailRequest.getEmailAddress(), emailRequest.getSubject(),
                        emailRequest.getMessage());
            } catch (MailException ex) {
                logger.error("Sending email is not possible after retry. Check email provider.", ex);
            }
        } else if (!emailRequest.getPayloadUri().isEmpty() && !emailRequest.getDataType().isEmpty()) {
            try {
                sendEmailWithAttachment(emailRequest.getEmailAddress(), emailRequest.getSubject(),
                        emailRequest.getMessage(), emailRequest.getPayloadUri().get(), emailRequest.getDataType().get());
            } catch (MailException ex) {
                logger.error("Sending email with attachment is not possible after retry. Check email provider.", ex);
            } catch (MessagingException mex) {
                logger.error("Creating email with attachment is not possible.", mex);
            } catch (URISyntaxException uex) {
                logger.error("Creating URI from payloadUri {} is not possible.", emailRequest.getPayloadUri().get(), uex);
            } catch (IOException ex) {
                logger.error("Downloading attachment for email to {} is not possible.", emailRequest.getEmailAddress(), ex);
            }
        } else {
            logger.warn("Sending email with attachment failed due to incomplete payload information (URI, data type) to email address {}", emailRequest.getEmailAddress());
        }
    }

    // annotation based retryable could not be unit tested sufficiently
    // maxRetries = 5 means up to 5 retries after the initial attempt -> total
    // attempts = 6
    private static final int MAX_RETRIES = 5;

    public void sendEmail(String to, String subject, String message) throws MailException {
        int attempts = 0;
        int maxAttempts = 1 + MAX_RETRIES;
        Optional<MailException> lastException = Optional.empty();

        // TODO remove test code
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

        // TODO remove test code
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