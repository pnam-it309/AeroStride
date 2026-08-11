package com.example.be.core.customer.lienhe.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLienHeRequest {

    @NotBlank(message = "Vui lòng nhập họ và tên!")
    private String ten;

    @NotBlank(message = "Vui lòng nhập số điện thoại!")
    private String sdt;

    private String email;

    private String chuDe;

    @NotBlank(message = "Vui lòng nhập nội dung cần hỗ trợ!")
    private String noiDung;
}
