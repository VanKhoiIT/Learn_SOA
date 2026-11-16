package com.example.demo.controller;

import com.example.demo.dto.OrderRequest;
import com.example.demo.entity.Order;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 1. GET /orders: Lay danh sach tat ca cac don hang
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // 2. GET /orders/{id}: Lay thong tin chi tiet mot don hang
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable("id") Long id) {
        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isPresent()) {
            return ResponseEntity.ok(orderOpt.get());
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "Khong tim thay don hang co ID: " + id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // 3. POST /orders: Tao don hang moi (kiem tra ton kho qua Product Service)
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request, HttpServletRequest httpRequest) {
        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpStatus.BAD_REQUEST.value());
            error.put("error", "Bad Request");
            error.put("message", "Ten khach hang khong duoc de trong!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        if (request.getCustomerEmail() == null || request.getCustomerEmail().trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpStatus.BAD_REQUEST.value());
            error.put("error", "Bad Request");
            error.put("message", "Email khach hang khong duoc de trong!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        String token = extractToken(httpRequest);

        try {
            Order createdOrder = orderService.createOrder(request, token);

            Map<String, Object> response = new HashMap<>();
            response.put("status", HttpStatus.CREATED.value());
            response.put("message", "Tao don hang moi thanh cong!");
            response.put("data", createdOrder);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpStatus.BAD_REQUEST.value());
            error.put("error", "Stock Validation Failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpStatus.BAD_REQUEST.value());
            error.put("error", "Bad Request");
            error.put("message", "Loi khi tao don hang: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // 4. PUT /orders/{id}: Cap nhat trang thai don hang
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable("id") Long id,
                                         @RequestBody OrderRequest request,
                                         HttpServletRequest httpRequest) {
        String token = extractToken(httpRequest);

        try {
            Optional<Order> updatedOrder = orderService.updateOrder(id, request, token);
            if (updatedOrder.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", HttpStatus.OK.value());
                response.put("message", "Cap nhat don hang ID " + id + " thanh cong!");
                response.put("data", updatedOrder.get());
                return ResponseEntity.ok(response);
            }

            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpStatus.NOT_FOUND.value());
            error.put("error", "Not Found");
            error.put("message", "Khong tim thay don hang co ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpStatus.BAD_REQUEST.value());
            error.put("error", "Bad Request");
            error.put("message", "Loi khi cap nhat don hang: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // 5. DELETE /orders/{id}: Xoa mot don hang
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        String token = extractToken(httpRequest);
        boolean deleted = orderService.deleteOrder(id, token);

        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", HttpStatus.OK.value());
            response.put("message", "Xoa don hang co ID " + id + " thanh cong!");
            return ResponseEntity.ok(response);
        }

        Map<String, Object> error = new HashMap<>();
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", "Khong tim thay don hang co ID: " + id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    private String extractToken(HttpServletRequest request) {
        Object tokenAttr = request.getAttribute("jwtToken");
        if (tokenAttr != null) {
            return tokenAttr.toString();
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}

