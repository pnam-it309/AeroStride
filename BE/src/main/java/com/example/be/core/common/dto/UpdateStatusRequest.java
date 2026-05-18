package com.example.be.core.common.dto;

import com.example.be.infrastructure.constants.TrangThai;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chung cho các endpoint cập nhật trạng thái.
 * Thay thế Map<String, String> để có type safety và validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private TrangThai status;
}
