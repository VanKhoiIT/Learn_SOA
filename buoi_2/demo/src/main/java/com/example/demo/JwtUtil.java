package com.example.demo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    // Khóa bí mật tối thiểu 256-bit cho thuật toán HS256
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(
            "KhoaBiMatChoBaiThucHanhSo2SpringbootJwtSecurity2026".getBytes());
    // Thời hạn token: 1 tiếng
    private static final long EXPIRATION_TIME = 3600000;

    // Sinh Token
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    // Lấy thông tin (Claims) từ Token
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Kiểm tra token còn hạn và đúng chữ ký không
    public boolean validateToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}