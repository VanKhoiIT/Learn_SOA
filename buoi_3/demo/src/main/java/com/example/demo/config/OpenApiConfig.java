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
                        .title("SOA Buổi 3 - Dịch vụ Quản lý Sản phẩm (Product Management Service)")
                        .version("1.0.0")
                        .description("Tài liệu RESTful API dịch vụ Quản lý Sản phẩm theo kiến trúc SOA. Hoạt động trên cổng độc lập 8081, cơ sở dữ liệu riêng (H2/MySQL), và xác thực token qua Auth Service Buổi 2 (cổng 8080).")
                        .contact(new Contact().name("Trương Văn Khởi").email("truongvankhoi20055@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("BearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Dán chuỗi JWT Token lấy từ API POST http://localhost:8080/login ở Buổi 2")));
    }
}
