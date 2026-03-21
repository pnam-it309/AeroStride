package com.example.be.core.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadResult {
    private String fileUrl;
    private String publicId;
    private String format;
}
