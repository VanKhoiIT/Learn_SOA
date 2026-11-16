package com.example.demo.controller;

import com.example.demo.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthProxyController {

    @Value("${auth.service.url:http://localhost:8080/auth}")
    private String authServiceUrl;

    @Autowired
    private JwtUtil jwtUtil;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/login")
    public ResponseEntity<?> proxyLogin(@RequestBody Map<String, Object> body) {
        try {
            String targetUrl = authServiceUrl.replace("/auth", "/login");
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return ResponseEntity.status(response.statusCode())
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response.body());
        } catch (Exception e) {
            // Neu Auth Service chua mo, cho phep dang nhap test fallback
            String user = body.getOrDefault("userName", "admin").toString();
            String pass = body.getOrDefault("password", "123456").toString();

            if ("admin".equals(user) && "123456".equals(pass)) {
                String token = jwtUtil.generateToken(user);
                return ResponseEntity.ok(Map.of(
                        "token", token,
                        "message", "Dang nhap thanh cong qua che do fallback local!",
                        "note", "Auth Service 8080 chua khoi dong, su dung local token generator."
                ));
            }

            return ResponseEntity.status(401).body(Map.of("error", "Sai tai khoan hoac mat khau!"));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> proxyVerifyToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "Loi: Thieu Header Authorization: Bearer <token>"
            ));
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authServiceUrl))
                    .header("Authorization", authHeader)
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return ResponseEntity.status(response.statusCode()).body(Map.of(
                    "status", response.statusCode(),
                    "service", "Auth Service Buoi 2 (Port 8080)",
                    "endpoint", authServiceUrl,
                    "response", response.body(),
                    "valid", response.statusCode() == 200
            ));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of(
                    "status", 502,
                    "error", "Bad Gateway",
                    "message", "Khong the ket noi den Auth Service (8080): " + e.getMessage()
            ));
        }
    }
}
