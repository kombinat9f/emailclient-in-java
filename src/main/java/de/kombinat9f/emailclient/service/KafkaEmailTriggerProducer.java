package de.kombinat9f.emailclient.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import de.kombinat9f.emailclient.domain.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEmailTriggerProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendEmailRequest(EmailRequest emailRequest) {
        String emailRequestJson = convertToJson(emailRequest);
        kafkaTemplate.send("email-requests", emailRequestJson);
        log.info("Emailrequest added to kafka queue");
    }

    private String convertToJson(EmailRequest emailRequest) {
        return new ObjectMapper().writeValueAsString(emailRequest);
    }

}
