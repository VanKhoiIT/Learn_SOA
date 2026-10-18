package com.example.demo.controller;

import com.example.demo.dto.ProductRequest;
import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;
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
public class ProductController {

    @Autowired
    private ProductService productService;

    // 1. GET /products: Lay danh sach tat ca cac san pham
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    // 2. GET /products/{id}: Lay thong tin chi tiet mot san pham
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable("id") Long id) {
        Optional<Product> productOpt = productService.getProductById(id);
        if (productOpt.isPresent()) {
            return ResponseEntity.ok(productOpt.get());
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "Khong tim thay san pham co ID: " + id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // 3. POST /products: Them mot san pham moi
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Ten san pham khong duoc de trong!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Gia san pham phai lon hon hoac bang 0!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        if (request.getQuantity() != null && request.getQuantity() < 0) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "So luong san pham khong the am!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        Product createdProduct = productService.createProduct(request);

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.CREATED.value());
        response.put("message", "Them san pham moi thanh cong!");
        response.put("data", createdProduct);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 4. PUT /products/{id}: Cap nhat thong tin san pham
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable("id") Long id, @RequestBody ProductRequest request) {
        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "Gia san pham khong the am!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        if (request.getQuantity() != null && request.getQuantity() < 0) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", "So luong san pham khong the am!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        Optional<Product> updatedProductOpt = productService.updateProduct(id, request);
        if (updatedProductOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", HttpStatus.OK.value());
            response.put("message", "Cap nhat san pham thanh cong!");
            response.put("data", updatedProductOpt.get());
            return ResponseEntity.ok(response);
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "Khong tim thay san pham co ID: " + id + " de cap nhat!");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // 5. DELETE /products/{id}: Xoa mot san pham
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable("id") Long id) {
        boolean deleted = productService.deleteProduct(id);
        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", HttpStatus.OK.value());
            response.put("message", "Da xoa thanh cong san pham co ID: " + id);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "Khong tim thay san pham co ID: " + id + " de xoa!");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}
