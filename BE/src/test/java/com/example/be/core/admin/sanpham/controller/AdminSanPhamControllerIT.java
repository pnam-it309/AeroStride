package com.example.be.core.admin.sanpham.controller;

import com.example.be.BaseIntegrationTest;
import com.example.be.core.admin.sanpham.repository.AdminSanPhamRepository;
import com.example.be.entity.SanPham;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.utils.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Automation API Test for Product Module.
 * Tests end-to-end flow from Controller to Database.
 */
class AdminSanPhamControllerIT extends BaseIntegrationTest {

    @Autowired
    private AdminSanPhamRepository sanPhamRepository;

    @BeforeEach
    void setUp() {
        sanPhamRepository.deleteAll();
    }

    @Test
    @DisplayName("Automation API: GET /san-pham - Should return list of products from real DB")
    @WithMockUser(roles = "ADMIN")
    void getProducts_Success() throws Exception {
        // Arrange: Insert real data to test DB
        SanPham sp1 = TestDataFactory.createMockSanPham();
        sp1.setTen("Giày Nike Pegasus");
        sanPhamRepository.save(sp1);

        // Act & Assert
        mockMvc.perform(get(RoutesConstant.ADMIN_SAN_PHAM)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].ten").value("Giày Nike Pegasus"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
