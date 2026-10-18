package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private AuthServiceCommunicator authServiceCommunicator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Cho phép truy cập tự do vào giao diện Web Demo, Swagger UI và H2 Console
        if (path.equals("/") ||
            path.equals("/index.html") ||
            path.startsWith("/static") ||
            path.endsWith(".html") ||
            path.endsWith(".js") ||
            path.endsWith(".css") ||
            path.startsWith("/api/auth") ||
            path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") ||
            path.equals("/swagger-ui.html") ||
            path.startsWith("/h2-console") ||
            path.equals("/favicon.ico")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Kiểm tra Header Authorization cho các API nghiệp vụ (/products, ...)
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedError(response,
                    "Ban can cung cap Token hop le tu Auth Service (Buoi 2) qua Header Authorization: Bearer <token>");
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            sendUnauthorizedError(response, "Token rong! Vui long truyen Token hop le.");
            return;
        }

        // 3. Xác thực Token qua Auth Service Buổi 2 (SOA Inter-Service Call)
        AuthServiceCommunicator.AuthResult authResult = authServiceCommunicator.verifyTokenWithAuthService(token);

        if (authResult.isValid()) {
            // Lưu thông tin người dùng vào thuộc tính của request để controller có thể sử dụng nếu cần
            request.setAttribute("authenticatedUser", authResult.getUsername());
            filterChain.doFilter(request, response);
        } else {
            sendUnauthorizedError(response, authResult.getMessage());
        }
    }

    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String json = String.format("{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"%s\"}", message);
        response.getWriter().write(json);
    }
}
