package com.example.demo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class AuthServiceCommunicator {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceCommunicator.class);

    @Value("${auth.service.url:http://localhost:8080/auth}")
    private String authServiceUrl;

    @Value("${auth.service.fallback-local:true}")
    private boolean fallbackLocal;

    @Autowired
    private JwtUtil jwtUtil;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    // Xac thuc token qua Auth Service buoi 2 (chay o cong 8080)
    public AuthResult verifyTokenWithAuthService(String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authServiceUrl))
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                String username = "admin";
                if (body != null && body.contains("Xin chao: ")) {
                    username = body.substring(body.indexOf("Xin chao: ") + 10).trim();
                }
                log.info("Xac thuc thanh cong qua Auth Service buoi 2: user = {}", username);
                return new AuthResult(true, username, "Token hop le!");
            } else {
                return new AuthResult(false, null, "Token khong hop le hoac da het han!");
            }
        } catch (Exception e) {
            log.warn("Khong the ket noi truc tiep den Auth Service ({}), chuyen sang kiem tra JWT noi bo", authServiceUrl);
            if (fallbackLocal) {
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    return new AuthResult(true, username, "Token hop le!");
                } else {
                    return new AuthResult(false, null, "Token khong hop le hoac da het han!");
                }
            }
            return new AuthResult(false, null, "Khong the ket noi den Auth Service buoi 2!");
        }
    }

    public static class AuthResult {
        private final boolean valid;
        private final String username;
        private final String message;

        public AuthResult(boolean valid, String username, String message) {
            this.valid = valid;
            this.username = username;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getUsername() {
            return username;
        }

        public String getMessage() {
            return message;
        }
    }
}
