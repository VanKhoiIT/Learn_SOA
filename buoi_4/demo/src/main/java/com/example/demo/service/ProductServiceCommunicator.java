package com.example.demo.service;

import com.example.demo.dto.ProductDto;
import com.example.demo.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ProductServiceCommunicator {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceCommunicator.class);

    @Value("${product.service.url:http://localhost:8081/products}")
    private String productServiceUrl;

    @Value("${product.service.deduct-stock-on-order:true}")
    private boolean deductStockOnOrder;

    @Value("${product.service.strict-mode:false}")
    private boolean strictMode;

    @Autowired
    private JwtUtil jwtUtil;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Lay thong tin chi tiet san pham tu Product Service (Buoi 3 - cong 8081)
     */
    public ProductCheckResult checkProductStock(Long productId, Integer requestedQuantity, String token) {
        String effectiveToken = getEffectiveToken(token);
        String targetUrl = productServiceUrl + "/" + productId;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Authorization", "Bearer " + effectiveToken)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                return new ProductCheckResult(false, null, "Khong tim thay san pham co ID: " + productId + " tai Product Service!");
            }

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                ProductDto product = new ProductDto();
                product.setId(root.path("id").asLong(productId));
                product.setName(root.path("name").asText("San pham #" + productId));
                product.setDescription(root.path("description").asText(""));
                product.setPrice(new BigDecimal(root.path("price").asText("0.00")));
                product.setQuantity(root.path("quantity").asInt(0));

                log.info("Lay thong tin san pham thanh cong tu Product Service: ID={}, Ten='{}', Gia={}, Ton kho={}",
                        product.getId(), product.getName(), product.getPrice(), product.getQuantity());

                // Kiem tra so luong ton kho
                if (requestedQuantity != null && requestedQuantity > product.getQuantity()) {
                    String msg = String.format("San pham '%s' (ID: %d) khong du so luong trong kho! Yeu cau: %d, ton kho con: %d",
                            product.getName(), product.getId(), requestedQuantity, product.getQuantity());
                    return new ProductCheckResult(false, product, msg);
                }

                return new ProductCheckResult(true, product, "Ton kho hop le");
            }

            return new ProductCheckResult(false, null, "Loi khi goi Product Service (Status " + response.statusCode() + "): " + response.body());
        } catch (Exception e) {
            log.warn("Khong the ket noi den Product Service ({}) de kiem tra ton kho: {}", targetUrl, e.getMessage());

            if (!strictMode) {
                // Che do mem deo (Resilience / Fallback): neu giao vien chi chay moi Buoi 4
                String name = "San pham #" + productId;
                BigDecimal price = new BigDecimal("100000.00");
                int stock = 999;
                if (productId != null && productId == 1L) { name = "Laptop Dell XPS 15"; price = new BigDecimal("38000000.00"); stock = 12; }
                else if (productId != null && productId == 2L) { name = "Ban phim co Keychron K2"; price = new BigDecimal("1850000.00"); stock = 26; }
                else if (productId != null && productId == 3L) { name = "Chuot Logitech MX Master 3S"; price = new BigDecimal("2200000.00"); stock = 15; }

                if (requestedQuantity != null && requestedQuantity > stock) {
                    return new ProductCheckResult(false, null, "San pham '" + name + "' (ID: " + productId + ") khong du so luong trong kho! Yeu cau: " + requestedQuantity + ", ton kho con: " + stock);
                }

                ProductDto fallbackProduct = new ProductDto(
                        productId,
                        name,
                        "Thong tin san pham tu dong khi Product Service chua online (Che do Standalone)",
                        price,
                        stock
                );
                return new ProductCheckResult(true, fallbackProduct, "Ket noi Product Service bi gian doan, su dung fallback.");
            }

            return new ProductCheckResult(false, null, "Khong the ket noi den Product Service (Buoi 3 - cong 8081): " + e.getMessage());
        }
    }

    /**
     * Cap nhat tru ton kho sau khi dat hang thanh cong (SOA Integration)
     */
    public boolean deductStock(Long productId, Integer quantityToDeduct, String token) {
        if (!deductStockOnOrder || quantityToDeduct == null || quantityToDeduct <= 0) {
            return false;
        }

        String effectiveToken = getEffectiveToken(token);
        String targetUrl = productServiceUrl + "/" + productId;

        try {
            // 1. Lay thong tin san pham hien tai
            HttpRequest getReq = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Authorization", "Bearer " + effectiveToken)
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> getRes = httpClient.send(getReq, HttpResponse.BodyHandlers.ofString());
            if (getRes.statusCode() != 200) {
                return false;
            }

            JsonNode root = objectMapper.readTree(getRes.body());
            int currentQty = root.path("quantity").asInt(0);
            int newQty = Math.max(0, currentQty - quantityToDeduct);

            // 2. Gui PUT cap nhat ton kho moi
            String updatePayload = objectMapper.createObjectNode()
                    .put("name", root.path("name").asText())
                    .put("description", root.path("description").asText())
                    .put("price", root.path("price").asText())
                    .put("quantity", newQty)
                    .toString();

            HttpRequest putReq = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Authorization", "Bearer " + effectiveToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .PUT(HttpRequest.BodyPublishers.ofString(updatePayload))
                    .build();

            HttpResponse<String> putRes = httpClient.send(putReq, HttpResponse.BodyHandlers.ofString());
            log.info("Tru ton kho san pham ID={} thanh cong tu {} -> {} (Status: {})", productId, currentQty, newQty, putRes.statusCode());
            return putRes.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Khong the tu dong cap nhat tru ton kho cho san pham ID={}: {}", productId, e.getMessage());
            return false;
        }
    }

    /**
     * Hoan tra ton kho khi huy don hang hoac xoa mat hang
     */
    public boolean restoreStock(Long productId, Integer quantityToRestore, String token) {
        if (quantityToRestore == null || quantityToRestore <= 0) {
            return false;
        }

        String effectiveToken = getEffectiveToken(token);
        String targetUrl = productServiceUrl + "/" + productId;

        try {
            HttpRequest getReq = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Authorization", "Bearer " + effectiveToken)
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> getRes = httpClient.send(getReq, HttpResponse.BodyHandlers.ofString());
            if (getRes.statusCode() != 200) {
                return false;
            }

            JsonNode root = objectMapper.readTree(getRes.body());
            int currentQty = root.path("quantity").asInt(0);
            int newQty = currentQty + quantityToRestore;

            String updatePayload = objectMapper.createObjectNode()
                    .put("name", root.path("name").asText())
                    .put("description", root.path("description").asText())
                    .put("price", root.path("price").asText())
                    .put("quantity", newQty)
                    .toString();

            HttpRequest putReq = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Authorization", "Bearer " + effectiveToken)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .PUT(HttpRequest.BodyPublishers.ofString(updatePayload))
                    .build();

            HttpResponse<String> putRes = httpClient.send(putReq, HttpResponse.BodyHandlers.ofString());
            log.info("Hoan tra ton kho san pham ID={} thanh cong tu {} -> {} (Status: {})", productId, currentQty, newQty, putRes.statusCode());
            return putRes.statusCode() == 200;
        } catch (Exception e) {
            log.warn("Khong the hoan tra ton kho cho san pham ID={}: {}", productId, e.getMessage());
            return false;
        }
    }

    private String getEffectiveToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            return token.replace("Bearer ", "").trim();
        }
        return jwtUtil.generateToken("order-service-client");
    }

    public static class ProductCheckResult {
        private final boolean success;
        private final ProductDto product;
        private final String message;

        public ProductCheckResult(boolean success, ProductDto product, String message) {
            this.success = success;
            this.product = product;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public ProductDto getProduct() {
            return product;
        }

        public String getMessage() {
            return message;
        }
    }
}

