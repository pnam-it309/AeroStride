package com.example.be.core.admin.danhgia.model.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDanhGiaConfigResponse {
    private boolean autoApprove;
    private long total;
    private long pending;
    private long approved;
    private long rejected;
    private long spam;
}
