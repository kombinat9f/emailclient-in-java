package de.kombinat9f.emailclient.service;

import java.util.Optional;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import de.kombinat9f.emailclient.client.AttachmentClient;
import de.kombinat9f.emailclient.domain.EmailRequest;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSenderService {
    
    private static final String DEFAULT_ORIGIN_COM = "default@origin.com";
    private static final int MAX_RETRIES_AFTER_INITIAL = 5;

    @Value("${from.email}")
    private String from;

    private final JavaMailSender mailSender;
    private final AttachmentClient restClient;

    public void sendOneEmail(EmailRequest emailRequest) {
        
        if (emailRequest == null) {
            log.warn("Email data is faulty. There could be no email created.");
        } else if (emailRequest.getPayloadUri() == null) {
            try {
                sendEmail(emailRequest.getEmailAddress(), emailRequest.getSubject(),
                        emailRequest.getMessage());
            } catch (MailException ex) {
                log.error("Sending email is not possible after retry. Check email provider.", ex);
            }
        } else if (emailRequest.getPayloadUri() != null && emailRequest.getDataType() != null) {
            try {
                sendEmailWithAttachment(emailRequest.getEmailAddress(), emailRequest.getSubject(),
                        emailRequest.getMessage(), emailRequest.getPayloadUri(), emailRequest.getDataType());
            } catch (MailException ex) {
                log.error("Sending email with attachment is not possible after retry. Check email provider.", ex);
            } catch (MessagingException mex) {
                log.error("Creating email with attachment is not possible.", mex);
            } catch (URISyntaxException uex) {
                log.error("Creating URI from payloadUri {} is not possible.", emailRequest.getPayloadUri(), uex);
            } catch (IOException ex) {
                log.error("Downloading attachment for email to {} is not possible.", emailRequest.getEmailAddress(), ex);
            }
        } else {
            log.warn("Sending email with attachment failed due to incomplete payload information (URI, data type) to email address {}", emailRequest.getEmailAddress());
        }
    }

    public void sendEmail(String to, String subject, String message) throws MailException {
        int attempts = 0;
        Optional<MailException> lastException = Optional.empty();

        if (from == null) {
            from = DEFAULT_ORIGIN_COM;
        }
        while (attempts <= MAX_RETRIES_AFTER_INITIAL) {
            try {
                SimpleMailMessage email = new SimpleMailMessage();
                email.setTo(to);
                email.setSubject(subject);
                email.setText(message);
                email.setFrom(from);
                mailSender.send(email);
                return;
            } catch (MailException ex) {
                attempts++;
                lastException = Optional.of(ex);
                log.warn("Attempt {} to send email failed", attempts, ex);
            }
        }
        lastException.ifPresent(value -> {
            throw value;
        });
    }

    private void sendEmailWithAttachment(String to, String subject, String message, String payloadUri, String dataType)
            throws MessagingException, MailException, URISyntaxException, IOException {

        int attempts = 0;
        Optional<MailException> lastException = Optional.empty();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        URI sanitizedUri = new URI(payloadUri).normalize();

        if (from == null) {
            from = DEFAULT_ORIGIN_COM;
        }

        while (attempts <= MAX_RETRIES_AFTER_INITIAL) {
            try {
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(message);
                helper.setFrom(from);
               
                byte[] file = restClient.prepareAttachmentFile(sanitizedUri);
                DataSource dataSource = new ByteArrayDataSource(file, dataType);
                helper.addAttachment("Attachment", dataSource);
                
                mailSender.send(mimeMessage);
                return;
            } catch (MailException ex) {
                attempts++;
                lastException = Optional.of(ex);
                log.warn("Attempt {} to send email failed", attempts, ex);
            }
        }
        lastException.ifPresent(value -> {
            throw value;
        });
    }
}