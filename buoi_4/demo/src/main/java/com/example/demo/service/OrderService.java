package com.example.demo.service;

import com.example.demo.dto.OrderItemRequest;
import com.example.demo.dto.OrderRequest;
import com.example.demo.dto.ProductDto;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductServiceCommunicator productServiceCommunicator;

    @PostConstruct
    public void initSampleData() {
        if (orderRepository.count() == 0) {
            Order sampleOrder = new Order(
                    "Tran Van B",
                    "tranvanb@gmail.com",
                    new BigDecimal("36850000.00"),
                    "pending"
            );

            OrderItem item1 = new OrderItem(
                    sampleOrder,
                    1L,
                    "Laptop Dell XPS 15",
                    1,
                    new BigDecimal("35000000.00")
            );

            OrderItem item2 = new OrderItem(
                    sampleOrder,
                    2L,
                    "Ban phim co Keychron K2",
                    1,
                    new BigDecimal("1850000.00")
            );

            sampleOrder.addItem(item1);
            sampleOrder.addItem(item2);

            orderRepository.save(sampleOrder);
        }
    }

    // 1. Lay danh sach tat ca don hang
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // 2. Lay thong tin chi tiet mot don hang
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    // 3. Tao don hang moi (kiem tra ton kho qua Product Service)
    @Transactional
    public Order createOrder(OrderRequest request, String token) {
        Order order = new Order();
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            String st = request.getStatus().trim().toLowerCase();
            if (!List.of("pending", "completed", "cancelled").contains(st)) {
                throw new IllegalArgumentException("Trang thai don hang khong hop le! Chi chap nhan: pending, completed, cancelled");
            }
            order.setStatus(st);
        } else {
            order.setStatus("pending");
        }

        BigDecimal calculatedTotal = BigDecimal.ZERO;

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (OrderItemRequest itemReq : request.getItems()) {
                if (itemReq.getProductId() == null) {
                    throw new IllegalArgumentException("Thieu productId cho mat hang!");
                }
                if (itemReq.getQuantity() != null && itemReq.getQuantity() <= 0) {
                    throw new IllegalArgumentException("So luong san pham dat mua phai lon hon 0!");
                }
                int quantity = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;

                // Goi Product Service kiem tra san pham va ton kho
                ProductServiceCommunicator.ProductCheckResult checkResult =
                        productServiceCommunicator.checkProductStock(itemReq.getProductId(), quantity, token);

                if (!checkResult.isSuccess()) {
                    throw new IllegalStateException(checkResult.getMessage());
                }

                ProductDto product = checkResult.getProduct();
                String productName = (itemReq.getProductName() != null && !itemReq.getProductName().trim().isEmpty())
                        ? itemReq.getProductName()
                        : (product != null ? product.getName() : "San pham #" + itemReq.getProductId());

                BigDecimal unitPrice = itemReq.getUnitPrice() != null
                        ? itemReq.getUnitPrice()
                        : (product != null && product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);

                OrderItem item = new OrderItem(order, itemReq.getProductId(), productName, quantity, unitPrice);
                order.addItem(item);

                calculatedTotal = calculatedTotal.add(item.getTotalPrice());

                // Tru ton kho ben Product Service
                productServiceCommunicator.deductStock(itemReq.getProductId(), quantity, token);
            }
            order.setTotalAmount(calculatedTotal);
        } else {
            order.setTotalAmount(request.getTotalAmount() != null ? request.getTotalAmount() : BigDecimal.ZERO);
        }

        return orderRepository.save(order);
    }

    // 4. Cap nhat thong tin / trang thai don hang
    @Transactional
    public Optional<Order> updateOrder(Long id, OrderRequest request, String token) {
        return orderRepository.findById(id).map(order -> {
            if (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()) {
                order.setCustomerName(request.getCustomerName());
            }
            if (request.getCustomerEmail() != null && !request.getCustomerEmail().trim().isEmpty()) {
                order.setCustomerEmail(request.getCustomerEmail());
            }
            if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                String oldStatus = order.getStatus();
                String newStatus = request.getStatus().trim().toLowerCase();
                if (!List.of("pending", "completed", "cancelled").contains(newStatus)) {
                    throw new IllegalArgumentException("Trang thai don hang khong hop le! Chi chap nhan: pending, completed, cancelled");
                }
                order.setStatus(newStatus);

                // Neu chuyen sang 'cancelled', hoan tra ton kho
                if (!"cancelled".equalsIgnoreCase(oldStatus) && "cancelled".equalsIgnoreCase(newStatus)) {
                    for (OrderItem item : order.getItems()) {
                        productServiceCommunicator.restoreStock(item.getProductId(), item.getQuantity(), token);
                    }
                } else if ("cancelled".equalsIgnoreCase(oldStatus) && !"cancelled".equalsIgnoreCase(newStatus)) {
                    // Neu kich hoat lai don da huy, tru lai ton kho
                    for (OrderItem item : order.getItems()) {
                        productServiceCommunicator.deductStock(item.getProductId(), item.getQuantity(), token);
                    }
                }
            }
            if (request.getTotalAmount() != null) {
                order.setTotalAmount(request.getTotalAmount());
            }
            return orderRepository.save(order);
        });
    }

    // 5. Xoa mot don hang
    @Transactional
    public boolean deleteOrder(Long id, String token) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            // Hoan tra ton kho neu don hang chua hoan thanh
            if (!"completed".equalsIgnoreCase(order.getStatus())) {
                for (OrderItem item : order.getItems()) {
                    productServiceCommunicator.restoreStock(item.getProductId(), item.getQuantity(), token);
                }
            }
            orderRepository.delete(order);
            return true;
        }
        return false;
    }

    // Tinh toan lai tong tien don hang khi them/sua/xoa order_item
    @Transactional
    public void recalculateOrderTotal(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
            BigDecimal total = BigDecimal.ZERO;
            for (OrderItem it : items) {
                if (it.getTotalPrice() != null) {
                    total = total.add(it.getTotalPrice());
                }
            }
            order.setTotalAmount(total);
            orderRepository.save(order);
        });
    }
}

