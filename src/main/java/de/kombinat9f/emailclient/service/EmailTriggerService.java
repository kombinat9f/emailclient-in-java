package de.kombinat9f.emailclient.service;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import de.kombinat9f.emailclient.domain.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailTriggerService {

    private static final String TO = "to";
    private static final String SUBJECT = "subject";
    private static final String MESSAGE = "message";
    private static final String PAYLOAD_URI = "payloadUri";
    private static final String DATA_TYPE = "dataType";

    private static String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    private final KafkaEmailTriggerProducer emailProducerService;

    public void produceEmailTrigger(Map<String, String> emailContent) {
        Optional<EmailRequest> emailData = sanitizeData(emailContent);

        if (emailData.isPresent()) {
            emailProducerService.sendEmailRequest(emailData.get());
        }
    }

    private Optional<EmailRequest> sanitizeData(Map<String, String> emailContent) {

        String emailAddress = null;
        String subject = null;
        String message = null;
        String payloadUri = null;
        String dataType = null;

        if (StringUtils.isNotEmpty(emailContent.get(TO))) {
            emailAddress = emailContent.get(TO);

            if (!emailAddress.matches(EMAIL_PATTERN)) {
                log.warn("Invalid format for Email-Address: {}", emailAddress);
            }
        } else {
            log.warn("Missing Email address");
        }

        if (StringUtils.isNotEmpty(emailContent.get(SUBJECT))) {
            subject = emailContent.get(SUBJECT);
        } else {
            log.warn("Invalid format for subject for Email to: {}", emailAddress);
        }
        if (StringUtils.isNotEmpty(emailContent.get(MESSAGE))) {
            message = emailContent.get(MESSAGE);
        } else {
            log.warn("Invalid format for message for Email to: {}", emailAddress);
        }
        if (StringUtils.isNotEmpty(emailContent.get(PAYLOAD_URI))
                && StringUtils.isNotEmpty(emailContent.get(DATA_TYPE))) {
            payloadUri = emailContent.get(PAYLOAD_URI);
            dataType = emailContent.get(DATA_TYPE);
        }

        EmailRequest emailRequest = EmailRequest.builder()
                .emailAddress(emailAddress)
                .subject(subject)
                .message(message)
                .dataType(dataType)
                .payloadUri(payloadUri).build();

        return Optional.of(emailRequest);
    }

}
