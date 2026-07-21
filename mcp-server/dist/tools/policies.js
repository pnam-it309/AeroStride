"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.policiesTool = void 0;
exports.policiesTool = {
    name: "getStorePolicies",
    description: "Lấy chính sách của cửa hàng AeroStride (bảo hành, đổi trả, v.v.)",
    parameters: {},
    handler: async () => {
        const policies = `
CHÍNH SÁCH CỬA HÀNG AEROSTRIDE:
1. Chính sách bảo hành:
- Bảo hành 6 tháng cho các lỗi kỹ thuật từ nhà sản xuất (keo, chỉ).
- Không bảo hành lỗi do người dùng (trầy xước, mòn đế, cháy xém).
2. Chính sách đổi trả:
- Đổi trả miễn phí trong vòng 7 ngày nếu giữ nguyên tem mác, hộp và chưa qua sử dụng.
- Không áp dụng đổi trả cho các sản phẩm trong chương trình khuyến mãi giảm giá quá 30%.
3. Giờ mở cửa:
- 8h00 - 22h00 các ngày trong tuần.
`;
        return {
            content: [{ type: "text", text: policies }]
        };
    }
};
