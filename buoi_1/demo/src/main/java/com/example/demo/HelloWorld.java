package com.example.demo; // Nếu package của bạn khác tên, hãy sửa lại cho đúng với file HelloworldApplication.java

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorld {

    // Khi người dùng truy cập đường dẫn gốc http://localhost:8080/
    @GetMapping("/")
    public String index() {
        return "Hello World! Spring Boot da chay thanh cong tren VS Code!";
    }

    // Một API khác: http://localhost:8080/api/test
    @GetMapping("/api/test")
    public String test() {
        return "Day la endpoint API thu 2!";
    }
}