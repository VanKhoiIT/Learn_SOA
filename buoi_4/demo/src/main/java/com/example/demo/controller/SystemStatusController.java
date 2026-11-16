package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system-status")
@CrossOrigin(origins = "*")
public class SystemStatusController {

    private static final Logger log = LoggerFactory.getLogger(SystemStatusController.class);

    @Value("${auth.service.url:http://localhost:8080/auth}")
    private String authServiceUrl;

    @Value("${product.service.url:http://localhost:8081/products}")
    private String productServiceUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(1500))
            .build();

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSystemOperatingMode() {
        Map<String, Object> result = new HashMap<>();

        // 1. Kiem tra Auth Service (Buoi 2 - Cong 8080)
        boolean authOnline = checkService(authServiceUrl);
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put("port", 8080);
        authInfo.put("url", authServiceUrl);
        authInfo.put("online", authOnline);
        authInfo.put("status", authOnline ? "ONLINE" : "OFFLINE");
        authInfo.put("mode", authOnline
                ? "Kết nối trực tiếp Auth Service (Port 8080)"
                : "Đang chạy chế độ Cục bộ Giả lập (Local Fallback)");
        result.put("authService", authInfo);

        // 2. Kiem tra Product Service (Buoi 3 - Cong 8081)
        boolean productOnline = checkService(productServiceUrl);
        Map<String, Object> productInfo = new HashMap<>();
        productInfo.put("port", 8081);
        productInfo.put("url", productServiceUrl);
        productInfo.put("online", productOnline);
        productInfo.put("status", productOnline ? "ONLINE" : "OFFLINE");
        productInfo.put("mode", productOnline
                ? "Kết nối trực tiếp Product Service (Port 8081)"
                : "Đang chạy chế độ Cục bộ Giả lập (Local Fallback)");
        result.put("productService", productInfo);

        // 3. Che do van hanh tong the
        if (authOnline && productOnline) {
            result.put("operatingMode", "FULL_SOA_LIVE");
            result.put("bannerTitle", "🌐 CHẾ ĐỘ KẾT NỐI TOÀN DIỆN (FULL SOA LIVE)");
            result.put("bannerMessage", "Đã kết nối thành công với Auth Service (8080) và Product Service (8081). Mọi dữ liệu và tồn kho được đồng bộ thật qua mạng!");
            result.put("badgeClass", "badge-green");
        } else if (!authOnline && !productOnline) {
            result.put("operatingMode", "STANDALONE_FALLBACK");
            result.put("bannerTitle", "⚠️ CHẾ ĐỘ CHẠY CỤC BỘ GIẢ LẬP (STANDALONE MOCK)");
            result.put("bannerMessage", "Phát hiện Buổi 2 và Buổi 3 chưa khởi động. Buổi 4 đang tự động kích hoạt cơ chế giả lập cục bộ để bạn vẫn kiểm thử đầy đủ 100% chức năng!");
            result.put("badgeClass", "badge-amber");
        } else {
            result.put("operatingMode", "PARTIAL_HYBRID");
            result.put("bannerTitle", "⚡ CHẾ ĐỘ HỖN HỢP (PARTIAL HYBRID)");
            result.put("bannerMessage", String.format("Buổi 2: %s | Buổi 3: %s. Hệ thống tự động kích hoạt dự phòng cho dịch vụ đang offline.",
                    authOnline ? "KẾT NỐI THẬT" : "GIẢ LẬP CỤC BỘ",
                    productOnline ? "KẾT NỐI THẬT" : "GIẢ LẬP CỤC BỘ"));
            result.put("badgeClass", "badge-blue");
        }

        return ResponseEntity.ok(result);
    }

    private boolean checkService(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(1500))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            // Neu tra ve bat ky ma nao khac connection refused (ke ca 401 Unauthorized vi chua gui token), nghia la service DANG CHAY
            return response.statusCode() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}

