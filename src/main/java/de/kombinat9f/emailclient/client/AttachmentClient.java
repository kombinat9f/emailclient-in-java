package de.kombinat9f.emailclient.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AttachmentClient {

    private final RestClient restClient = RestClient.create();

    public byte[] prepareAttachmentFile(URI uriToFile) throws IOException {
        // basic solution for small to average file sizes
        return restClient.get().uri(uriToFile).retrieve().body(byte[].class);
    }
}
