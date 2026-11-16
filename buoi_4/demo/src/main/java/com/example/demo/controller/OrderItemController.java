package com.example.demo.controller;

import com.example.demo.dto.OrderItemRequest;
import com.example.demo.entity.OrderItem;
import com.example.demo.service.OrderItemService;
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
@RequestMapping("/order_items")
@CrossOrigin(origins = "*")
public class OrderItemController {

    @Autowired
    private OrderItemService orderItemService;

    // 1. GET /order_items: Lay danh sach tat ca mat hang trong don hang (co the truyen ?order_id=...)
    @GetMapping
    public ResponseEntity<List<OrderItem>> getAllOrderItems(@RequestParam(value = "order_id", required = false) Long orderId) {
        List<OrderItem> items = orderItemService.getAllOrderItems(orderId);
        return ResponseEntity.ok(items);
    }

    // 2. GET /order_items/{id}: Lay thong tin chi tiet mot mat hang
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderItemById(@PathVariable("id") Long id) {
        Optional<OrderItem> itemOpt = orderItemService.getOrderItemById(id);
        if (itemOpt.isPresent()) {
            return ResponseEntity.ok(itemOpt.get());
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "Khong tim thay mat hang co ID: " + id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // 3. POST /order_items: Tao mat hang moi (kiem tra ton kho tu Product Service)
    @PostMapping
    public ResponseEntity<?> createOrderItem(@RequestBody OrderItemRequest request, HttpServletRequest httpRequest) {
        String token = extractToken(httpRequest);

        try {
            OrderItem createdItem = orderItemService.createOrderItem(request, token);

            Map<String, Object> response = new HashMap<>();
            response.put("status", HttpStatus.CREATED.value());
            response.put("message", "Them mat hang vao don hang thanh cong!");
            response.put("data", createdItem);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpStatus.BAD_REQUEST.value());
            error.put("error", "Bad Request");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpStatus.BAD_REQUEST.value());
            error.put("error", "Stock Validation Failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            error.put("error", "Internal Server Error");
            error.put("message", "Loi khi them mat hang: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // 4. PUT /order_items/{id}: Cap nhat mat hang
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrderItem(@PathVariable("id") Long id,
                                             @RequestBody OrderItemRequest request,
                                             HttpServletRequest httpRequest) {
        String token = extractToken(httpRequest);

        try {
            Optional<OrderItem> updatedItem = orderItemService.updateOrderItem(id, request, token);
            if (updatedItem.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", HttpStatus.OK.value());
                response.put("message", "Cap nhat mat hang ID " + id + " thanh cong!");
                response.put("data", updatedItem.get());
                return ResponseEntity.ok(response);
            }

            Map<String, Object> error = new HashMap<>();
            error.put("status", HttpStatus.NOT_FOUND.value());
            error.put("error", "Not Found");
            error.put("message", "Khong tim thay mat hang co ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
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
            error.put("message", "Loi khi cap nhat mat hang: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // 5. DELETE /order_items/{id}: Xoa mot mat hang trong don hang
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrderItem(@PathVariable("id") Long id, HttpServletRequest httpRequest) {
        String token = extractToken(httpRequest);
        boolean deleted = orderItemService.deleteOrderItem(id, token);

        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", HttpStatus.OK.value());
            response.put("message", "Xoa mat hang co ID " + id + " thanh cong (da hoan tra ton kho va cap nhat lai tong tien don hang)!");
            return ResponseEntity.ok(response);
        }

        Map<String, Object> error = new HashMap<>();
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", "Khong tim thay mat hang co ID: " + id);
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

