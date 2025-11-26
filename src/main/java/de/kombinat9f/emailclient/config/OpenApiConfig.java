package de.kombinat9f.emailclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {
    @Value("${openapi.dev-url}")
    private String url;

    @Bean
    public OpenAPI myOpenAPI() {
        Server server = new Server();
        server.setUrl(url);
        server.setDescription("Server URL");
        Info info = new Info()
                .title("Email client API")
                .version("1.0")
                .description(
                        "This API exposes an endpoint to to automatically send email with or without an attachment.");

        return new OpenAPI().info(info).addServersItem(server);
    }
}
