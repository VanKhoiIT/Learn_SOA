package com.example.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SOA Buoi 4 - Order Management Service API")
                        .version("1.0")
                        .description("RESTful API quan ly don hang va chi tiet don hang trong kien truc SOA, tich hop xac thuc Buoi 2 va kiem tra ton kho tu Product Service Buoi 3.")
                        .contact(new Contact().name("SOA Lab 4 Team").email("student@university.edu.vn")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("BearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhap Token JWT lay tu Auth Service Buoi 2 (POST http://localhost:8080/login)")));
    }
}

