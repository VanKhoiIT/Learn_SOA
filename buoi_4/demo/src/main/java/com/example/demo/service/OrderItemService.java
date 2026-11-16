package com.example.demo.service;

import com.example.demo.dto.OrderItemRequest;
import com.example.demo.dto.ProductDto;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductServiceCommunicator productServiceCommunicator;

    // 1. Lay danh sach mat hang (co ho tro loc theo orderId)
    public List<OrderItem> getAllOrderItems(Long orderId) {
        if (orderId != null) {
            return orderItemRepository.findByOrderId(orderId);
        }
        return orderItemRepository.findAll();
    }

    // 2. Lay chi tiet mot mat hang
    public Optional<OrderItem> getOrderItemById(Long id) {
        return orderItemRepository.findById(id);
    }

    // 3. Tao mat hang moi trong don hang (kiem tra ton kho tu Product Service)
    @Transactional
    public OrderItem createOrderItem(OrderItemRequest request, String token) {
        if (request.getOrderId() == null) {
            throw new IllegalArgumentException("Yeu cau can co 'orderId' de xac dinh don hang cha!");
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don hang co ID: " + request.getOrderId()));

        if (request.getProductId() == null) {
            throw new IllegalArgumentException("Yeu cau can co 'productId'!");
        }

        if (request.getQuantity() != null && request.getQuantity() <= 0) {
            throw new IllegalArgumentException("So luong san pham phai lon hon 0!");
        }

        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;

        // Kiem tra ton kho ben Product Service
        ProductServiceCommunicator.ProductCheckResult checkResult =
                productServiceCommunicator.checkProductStock(request.getProductId(), quantity, token);

        if (!checkResult.isSuccess()) {
            throw new IllegalStateException(checkResult.getMessage());
        }

        ProductDto product = checkResult.getProduct();
        String productName = (request.getProductName() != null && !request.getProductName().trim().isEmpty())
                ? request.getProductName()
                : (product != null ? product.getName() : "San pham #" + request.getProductId());

        BigDecimal unitPrice = request.getUnitPrice() != null
                ? request.getUnitPrice()
                : (product != null && product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);

        OrderItem newItem = new OrderItem(order, request.getProductId(), productName, quantity, unitPrice);
        order.addItem(newItem);
        OrderItem savedItem = orderItemRepository.save(newItem);

        // Tru ton kho Product Service
        productServiceCommunicator.deductStock(request.getProductId(), quantity, token);

        // Cap nhat tong tien don hang cha
        orderService.recalculateOrderTotal(order.getId());

        return savedItem;
    }

    // 4. Cap nhat mat hang
    @Transactional
    public Optional<OrderItem> updateOrderItem(Long id, OrderItemRequest request, String token) {
        return orderItemRepository.findById(id).map(existingItem -> {
            if (request.getProductName() != null && !request.getProductName().trim().isEmpty()) {
                existingItem.setProductName(request.getProductName());
            }

            if (request.getUnitPrice() != null) {
                existingItem.setUnitPrice(request.getUnitPrice());
            }

            if (request.getQuantity() != null && request.getQuantity() > 0 && !request.getQuantity().equals(existingItem.getQuantity())) {
                int oldQty = existingItem.getQuantity();
                int newQty = request.getQuantity();
                int diff = newQty - oldQty;

                if (diff > 0) {
                    // Can kiem tra xem kho con du de tang them khong
                    ProductServiceCommunicator.ProductCheckResult checkResult =
                            productServiceCommunicator.checkProductStock(existingItem.getProductId(), diff, token);
                    if (!checkResult.isSuccess()) {
                        throw new IllegalStateException(checkResult.getMessage());
                    }
                    productServiceCommunicator.deductStock(existingItem.getProductId(), diff, token);
                } else if (diff < 0) {
                    // Giam so luong thi hoan tra lai kho
                    productServiceCommunicator.restoreStock(existingItem.getProductId(), -diff, token);
                }

                existingItem.setQuantity(newQty);
            }

            OrderItem saved = orderItemRepository.save(existingItem);

            // Cap nhat tong tien don hang cha
            if (saved.getOrder() != null) {
                orderService.recalculateOrderTotal(saved.getOrder().getId());
            }

            return saved;
        });
    }

    // 5. Xoa mat hang trong don hang
    @Transactional
    public boolean deleteOrderItem(Long id, String token) {
        return orderItemRepository.findById(id).map(item -> {
            Order order = item.getOrder();
            Long orderId = order != null ? order.getId() : null;

            // Hoan tra ton kho
            productServiceCommunicator.restoreStock(item.getProductId(), item.getQuantity(), token);

            if (order != null) {
                order.removeItem(item);
            }
            orderItemRepository.delete(item);

            if (orderId != null) {
                orderService.recalculateOrderTotal(orderId);
            }
            return true;
        }).orElse(false);
    }
}

