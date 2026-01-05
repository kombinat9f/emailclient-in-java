package de.kombinat9f.emailclient.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailRequest {

    private String emailAddress;
    private String subject;
    private String message;
    private String dataType;
    private String payloadUri;
    
}
