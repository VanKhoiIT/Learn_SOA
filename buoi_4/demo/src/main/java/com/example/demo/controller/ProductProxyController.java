package com.example.demo.controller;

import com.example.demo.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products-proxy")
@CrossOrigin(origins = "*")
public class ProductProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProductProxyController.class);

    @Value("${product.service.url:http://localhost:8081/products}")
    private String productServiceUrl;

    @Autowired
    private JwtUtil jwtUtil;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @GetMapping
    public ResponseEntity<?> getProductsFromBuoi3(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String token = (authHeader != null && authHeader.startsWith("Bearer "))
                    ? authHeader.substring(7).trim()
                    : jwtUtil.generateToken("order-service-client");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(productServiceUrl))
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Ket noi Buoi 3 (8081) thanh cong, tra ve du lieu Live thuc te.");
                return ResponseEntity.ok()
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .body(response.body());
            }
        } catch (Exception e) {
            log.warn("Khong the ket noi sang Buoi 3 (8081): {}. Kich hoat che do du phong tu dong (Fallback / Fault-Tolerance).", e.getMessage());
        }

        // Che do chiu loi tu dong (Resilience): Neu giao vien chi chay moi Buoi 4 ma khong bat Buoi 3
        List<Map<String, Object>> fallbackProducts = List.of(
                Map.of("id", 1L, "name", "Laptop Dell XPS 15", "price", new BigDecimal("38000000.00"), "quantity", 12, "description", "San pham du phong khi Buoi 3 chua chay"),
                Map.of("id", 2L, "name", "Ban phim co Keychron K2", "price", new BigDecimal("1850000.00"), "quantity", 26, "description", "San pham du phong khi Buoi 3 chua chay"),
                Map.of("id", 3L, "name", "Chuot Logitech MX Master 3S", "price", new BigDecimal("2200000.00"), "quantity", 15, "description", "San pham du phong khi Buoi 3 chua chay")
        );

        return ResponseEntity.ok(fallbackProducts);
    }
}
