package com.example.demo;

import com.example.demo.dto.ProductRequest;
import com.example.demo.entity.Product;
import com.example.demo.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private String validToken;

    @BeforeEach
    void setUp() {
        // Sinh token hợp lệ sử dụng cùng secret key như Buổi 2
        validToken = "Bearer " + jwtUtil.generateToken("admin");
    }

    @Test
    @DisplayName("1. Truy cập GET /products khi không có Token -> Trả về 401 Unauthorized")
    void testGetAllProductsWithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("2. Truy cập GET /products khi có Token hợp lệ -> Trả về 200 OK và danh sách sản phẩm")
    void testGetAllProductsWithToken_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/products")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @DisplayName("3. Thêm mới sản phẩm POST /products -> Trả về 201 Created")
    void testCreateProduct_ShouldReturn201() throws Exception {
        ProductRequest request = new ProductRequest(
                "Màn hình LG 27 inch 4K",
                "Màn hình IPS hiển thị màu sắc chuyên nghiệp",
                new BigDecimal("9500000.00"),
                8
        );

        mockMvc.perform(post("/products")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.name").value("Màn hình LG 27 inch 4K"))
                .andExpect(jsonPath("$.data.price").value(9500000.00))
                .andExpect(jsonPath("$.data.quantity").value(8))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    @DisplayName("4. Lấy chi tiết sản phẩm GET /products/1 -> Trả về 200 OK")
    void testGetProductById_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/products/1")
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    @DisplayName("5. Lấy chi tiết sản phẩm không tồn tại GET /products/9999 -> Trả về 404 Not Found")
    void testGetProductNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/products/9999")
                        .header("Authorization", validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("6. Cập nhật thông tin sản phẩm PUT /products/1 -> Trả về 200 OK")
    void testUpdateProduct_ShouldReturn200() throws Exception {
        ProductRequest updateReq = new ProductRequest(
                "Laptop Dell XPS 15 (Ban nang cap 2026)",
                "Laptop chuyen dung cho lap trinh vien cao cap",
                new BigDecimal("38000000.00"),
                12
        );

        mockMvc.perform(put("/products/1")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("Laptop Dell XPS 15 (Ban nang cap 2026)"))
                .andExpect(jsonPath("$.data.price").value(38000000.00));
    }

    @Test
    @DisplayName("7. Xóa sản phẩm DELETE /products/{id} -> Trả về 200 OK")
    void testDeleteProduct_ShouldReturn200() throws Exception {
        // Tạo trước 1 sản phẩm để test xóa
        ProductRequest temp = new ProductRequest("San pham test xoa", "Mota", new BigDecimal("1000.00"), 1);
        String createResponse = mockMvc.perform(post("/products")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(temp)))
                .andReturn().getResponse().getContentAsString();

        // Lấy ID vừa tạo
        Number id = com.jayway.jsonpath.JsonPath.read(createResponse, "$.data.id");

        mockMvc.perform(delete("/products/" + id)
                        .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        // Kiểm tra lại xem sản phẩm đã bị xóa chưa
        mockMvc.perform(get("/products/" + id)
                        .header("Authorization", validToken))
                .andExpect(status().isNotFound());
    }
}
