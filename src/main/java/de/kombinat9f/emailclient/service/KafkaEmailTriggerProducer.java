package de.kombinat9f.emailclient.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import de.kombinat9f.emailclient.domain.EmailRequest;
import tools.jackson.databind.ObjectMapper;

@Service
public class KafkaEmailTriggerProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEmailTriggerProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEmailRequest(EmailRequest emailRequest) {
        String emailRequestJson = convertToJson(emailRequest);
        kafkaTemplate.send("email-requests", emailRequestJson);
    }

    private String convertToJson(EmailRequest emailRequest) {
        return new ObjectMapper().writeValueAsString(emailRequest);
    }

}
