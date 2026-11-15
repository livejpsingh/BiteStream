package com.fooddelivery.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI biteStreamOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BiteStream — Food Delivery & Dispatch Platform API")
                        .description("REST API for BiteStream: Real-time event-driven food delivery engine powered by " +
                                "Apache Kafka, PostgreSQL (Hybrid POJO + JSONB), Redis Cache, and Two-Step OTP Verification.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BiteStream Team")
                                .url("https://github.com/livejpsingh/BiteStream"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Current Environment Server"),
                        new Server().url("http://localhost:8080").description("Local Development Server")
                ));
    }
}
