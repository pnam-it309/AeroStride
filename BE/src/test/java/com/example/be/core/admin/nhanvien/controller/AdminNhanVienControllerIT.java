package com.example.be.core.admin.nhanvien.controller;

import com.example.be.BaseIntegrationTest;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminNhanVienControllerIT extends BaseIntegrationTest {

    @Test
    @DisplayName("API GET /v1/admin/nhan-vien - Should return success status (200)")
    @WithMockUser(roles = "ADMIN") // Mocking security context
    void getPage_Success() throws Exception {
        mockMvc.perform(get(RoutesConstant.ADMIN_NHAN_VIEN)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data").exists());
    }
}
