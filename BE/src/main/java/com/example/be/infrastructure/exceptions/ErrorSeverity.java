package com.example.be.infrastructure.exceptions;

/**
 * Classification of errors based on system requirements.
 */
public enum ErrorSeverity {
    FATAL,          // Lỗi chí mạng: Hệ thống dừng hoạt động
    RECOVERABLE,    // Lỗi có thể phục hồi: Chạy tiếp nhưng mất tính năng
    LOGICAL,        // Lỗi logic: Kết quả sai nhưng chương trình không sập
    SYNTAX,         // Lỗi cú pháp/Validate: Sai cấu trúc
    RUNTIME,        // Lỗi thực thi: Phát sinh khi đang chạy
    WARNING         // Cảnh báo: Nguy cơ tiềm ẩn
}
