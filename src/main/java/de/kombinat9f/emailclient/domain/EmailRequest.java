package de.kombinat9f.emailclient.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
@AllArgsConstructor
public class EmailRequest {

    private String emailAddress;
    private String subject;
    private String message;
    private String dataType;
    private String payloadUri;
    
}
