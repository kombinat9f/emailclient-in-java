package de.kombinat9f.emailclient.controller;

import java.io.IOException;
import java.net.URI;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AttachmentClient {

    RestClient restClient = RestClient.create();

    public byte[] prepareAttachmentFile(URI uriToFile) throws IOException {
        // not recommended for large files
        byte[] file = restClient.get().uri(uriToFile).retrieve().body(byte[].class);
        return file;
    }
}
