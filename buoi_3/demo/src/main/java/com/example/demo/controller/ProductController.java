package com.example.demo.controller;

import com.example.demo.dto.ProductRequest;
import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/products")
@Tag(name = "Product Management Controller", description = "Các API thực hiện CRUD quản lý sản phẩm (Yêu cầu Bearer Token từ Buổi 2)")
public class ProductController {

    @Autowired
    private ProductService productService;

    // 1. GET /products: Lấy danh sách tất cả các sản phẩm
    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả sản phẩm", description = "Trả về danh sách tất cả các sản phẩm trong cơ sở dữ liệu riêng của Product Service")
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    // 2. GET /products/{id}: Lấy thông tin chi tiết một sản phẩm
    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết một sản phẩm theo ID", description = "Tìm kiếm và trả về thông tin chi tiết sản phẩm theo mã ID")
    public ResponseEntity<?> getProductById(
            @Parameter(description = "ID của sản phẩm cần lấy thông tin", example = "1")
            @PathVariable("id") Long id) {
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isPresent()) {
            return ResponseEntity.ok(productOpt.get());
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "Không tìm thấy sản phẩm có ID = " + id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // 3. POST /products: Thêm một sản phẩm mới
    @PostMapping
    @Operation(summary = "Thêm một sản phẩm mới", description = "Tạo mới một sản phẩm và lưu vào cơ sở dữ liệu")
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
        // Kiểm tra dữ liệu đầu vào (Validation)
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Tên sản phẩm (name) không được để trống!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Giá sản phẩm (price) phải lớn hơn hoặc bằng 0!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        if (request.getQuantity() != null && request.getQuantity() < 0) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Số lượng sản phẩm trong kho (quantity) không thể là số âm!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        Product createdProduct = productService.createProduct(request);

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.CREATED.value());
        response.put("message", "Thêm sản phẩm mới thành công!");
        response.put("data", createdProduct);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 4. PUT /products/{id}: Cập nhật thông tin sản phẩm
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin sản phẩm theo ID", description = "Cập nhật tên, mô tả, giá hoặc số lượng của sản phẩm")
    public ResponseEntity<?> updateProduct(
            @Parameter(description = "ID của sản phẩm cần cập nhật", example = "1")
            @PathVariable("id") Long id,
            @RequestBody ProductRequest request) {

        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Giá sản phẩm không thể âm!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        if (request.getQuantity() != null && request.getQuantity() < 0) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Số lượng sản phẩm không thể âm!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        Optional<Product> updatedProductOpt = productService.updateProduct(id, request);
        if (updatedProductOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", HttpStatus.OK.value());
            response.put("message", "Cập nhật sản phẩm thành công!");
            response.put("data", updatedProductOpt.get());
            return ResponseEntity.ok(response);
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "Không tìm thấy sản phẩm có ID = " + id + " để cập nhật!");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // 5. DELETE /products/{id}: Xóa một sản phẩm
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa một sản phẩm theo ID", description = "Xóa vĩnh viễn sản phẩm khỏi cơ sở dữ liệu")
    public ResponseEntity<?> deleteProduct(
            @Parameter(description = "ID của sản phẩm cần xóa", example = "1")
            @PathVariable("id") Long id) {
        boolean deleted = productService.deleteProduct(id);
        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", HttpStatus.OK.value());
            response.put("message", "Đã xóa thành công sản phẩm có ID = " + id);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "Không tìm thấy sản phẩm có ID = " + id + " để xóa!");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
