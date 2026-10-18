package com.example.demo.service;

import com.example.demo.dto.ProductRequest;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // Khởi tạo một vài sản phẩm mẫu nếu database trống
    @PostConstruct
    public void initSampleData() {
        if (productRepository.count() == 0) {
            productRepository.save(new Product(
                    "Laptop Dell XPS 15",
                    "Laptop cao cap danh cho lap trinh vien va do hoa",
                    new BigDecimal("35000000.00"),
                    10
            ));
            productRepository.save(new Product(
                    "Ban phim co Keychron K2",
                    "Ban phim co khong day Bluetooth, Switch Gateron Brown",
                    new BigDecimal("1850000.00"),
                    25
            ));
            productRepository.save(new Product(
                    "Chuot Logitech MX Master 3S",
                    "Chuot khong day cong thai hoc, cam bien 8000 DPI",
                    new BigDecimal("2200000.00"),
                    15
            ));
        }
    }

    // 1. Lấy danh sách tất cả sản phẩm
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // 2. Lấy thông tin chi tiết một sản phẩm theo ID
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // 3. Thêm mới một sản phẩm
    public Product createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        return productRepository.save(product);
    }

    // 4. Cập nhật thông tin một sản phẩm theo ID
    public Optional<Product> updateProduct(Long id, ProductRequest request) {
        return productRepository.findById(id).map(existingProduct -> {
            if (request.getName() != null) {
                existingProduct.setName(request.getName());
            }
            if (request.getDescription() != null) {
                existingProduct.setDescription(request.getDescription());
            }
            if (request.getPrice() != null) {
                existingProduct.setPrice(request.getPrice());
            }
            if (request.getQuantity() != null) {
                existingProduct.setQuantity(request.getQuantity());
            }
            return productRepository.save(existingProduct);
        });
    }

    // 5. Xóa sản phẩm theo ID
    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
