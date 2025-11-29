package de.kombinat9f.emailclient.domain;

import java.util.Optional;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailRequest {

    private String emailAddress;
    private String subject;
    private String message;
    private Optional<String> dataType;
    private Optional<String> payloadUri;
    
}
