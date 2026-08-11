package com.example.be.core.customer.tintuc.model.request;

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
public class CustomerBinhLuanRequest {
    private String name;
    @NotBlank(message = "Vui lòng nhập nội dung bình luận!")
    private String text;
}
