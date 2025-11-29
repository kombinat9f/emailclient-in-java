package de.kombinat9f.emailclient.service;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.kombinat9f.emailclient.domain.EmailRequest;

@Service
public class EmailTriggerService {

    private static String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    Logger logger = LoggerFactory.getLogger(EmailTriggerService.class);

    private final KafkaEmailTriggerProducer emailProducerService;

    public EmailTriggerService(KafkaEmailTriggerProducer emailTriggerProducer) {
        this.emailProducerService = emailTriggerProducer;
    }

    public void produceEmailTrigger(Map<String, String> emailContent) {
        EmailRequest emailData = sanitizeData(emailContent);

        emailProducerService.sendEmailRequest(emailData);
    }

    private EmailRequest sanitizeData(Map<String, String> emailContent) {

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
            logger.warn("Missing Email address", new IllegalArgumentException());
            return null;
        }

        if (StringUtils.isNotEmpty(emailContent.get("subject"))) {
            subject = emailContent.get("subject");
        } else {
            logger.warn("Invalid format for subject for Email to: " + emailAddress, new IllegalArgumentException());
            return null;
        }
        if (StringUtils.isNotEmpty(emailContent.get("message"))) {
            message = emailContent.get("message");
        } else {
            logger.warn("Invalid format for message for Email to: " + emailAddress, new IllegalArgumentException());
            return null;
        }
        if (StringUtils.isNotEmpty(emailContent.get("payloadUri"))) {
            if (StringUtils.isNotEmpty(emailContent.get("dataType"))) {
                payloadUri = emailContent.get("payloadUri");
                dataType = emailContent.get("dataType");
            }
        }

        EmailRequest emailRequest = EmailRequest.builder()
        .emailAddress(emailAddress)
        .subject(subject)
        .message(message)
        .dataType(Optional.ofNullable(dataType))
        .payloadUri(Optional.ofNullable(payloadUri)).build();

        return emailRequest;
    }

}
