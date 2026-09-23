package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    // 1. API Đăng nhập: Trả về Token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Giả lập kiểm tra tài khoản theo bảng User trong đề bài
        // Lưu ý: client có thể gửi mật khẩu thường hoặc hash MD5/Base64
        if ("admin".equals(request.getUserName()) && "123456".equals(request.getPassword())) {
            String token = jwtUtil.generateToken(request.getUserName());
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Dang nhap thanh cong!");
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sai tai khoan hoac mat khau!");
    }

    // 2. API xác thực token: localhost:8080/auth
    @GetMapping("/auth")
    public ResponseEntity<?> verifyToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token khong hop le hoac thieu Header Authorization!");
        }

        String token = authHeader.substring(7);
        if (jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractClaims(token).getSubject();
            return ResponseEntity.ok("Token hop le! Xin chao: " + username);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token da het han hoac khong hop le!");
        }
    }
}