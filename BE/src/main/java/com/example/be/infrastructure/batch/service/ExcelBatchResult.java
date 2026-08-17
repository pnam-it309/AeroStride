package com.example.be.infrastructure.batch.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelBatchResult {
    private Long jobExecutionId;
    private String status;
    private long readCount;
    private long writeCount;
    private long skipCount;
    private long durationMs;
    private String message;
}
