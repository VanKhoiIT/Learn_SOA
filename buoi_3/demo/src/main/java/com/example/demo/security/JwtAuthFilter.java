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

        // Cho phep truy cap tu do vao cac duong dan nay
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

        // Kiem tra Header Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedError(response, "Loi: Ban can truyen Token hop le qua Header Authorization: Bearer <token>");
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            sendUnauthorizedError(response, "Loi: Token khong duoc de trong!");
            return;
        }

        // Xac thuc token qua Auth Service buoi 2
        AuthServiceCommunicator.AuthResult authResult = authServiceCommunicator.verifyTokenWithAuthService(token);

        if (authResult.isValid()) {
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
