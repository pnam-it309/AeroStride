"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.searchProductsTool = void 0;
const zod_1 = require("zod");
exports.searchProductsTool = {
    name: "searchProducts",
    description: "Tìm kiếm sản phẩm giày thông qua hệ thống Backend AeroStride API",
    parameters: {
        query: zod_1.z.string().describe("Từ khóa tìm kiếm (ví dụ: nike, giày chạy bộ)")
    },
    handler: async ({ query }) => {
        try {
            const backendHost = process.env.BACKEND_HOST || 'backend';
            const backendPort = process.env.BE_INTERNAL_PORT || process.env.BACKEND_PORT || '8080';
            const apiPrefix = process.env.APP_API_PREFIX || '/api/v1';
            const baseUrl = `http://${backendHost}:${backendPort}${apiPrefix}`;
            const apiUrl = `${baseUrl}/customer/san-pham/hien-thi?keyword=${encodeURIComponent(query)}&size=10`;
            console.log(`[ProductsTool] Searching API: ${apiUrl}`);
            const response = await fetch(apiUrl);
            if (!response.ok) {
                console.error(`[ProductsTool] Backend API returned status ${response.status}`);
                throw new Error(`Backend API returned status ${response.status}`);
            }
            const json = await response.json();
            const products = json.data?.items || [];
            if (products.length === 0) {
                return {
                    content: [{ type: "text", text: `Không tìm thấy sản phẩm nào khớp với từ khóa "${query}".` }]
                };
            }
            let resultText = `KẾT QUẢ TÌM KIẾM CHO "${query}":\n`;
            products.forEach((product) => {
                const brand = product.tenThuongHieu ? `[${product.tenThuongHieu}]` : '';
                const price = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.giaBanNhoNhat || 0);
                const discount = product.giaTriGiamGia ? ` (Giảm giá ${product.giaTriGiamGia}%)` : '';
                resultText += `- ${brand} ${product.tenSanPham} - Giá từ: ${price}${discount} - Đã bán: ${product.soLuongDaBan || 0}\n`;
            });
            return {
                content: [{ type: "text", text: resultText }]
            };
        }
        catch (error) {
            console.error("[ProductsTool] API search error:", error);
            return {
                content: [{ type: "text", text: "Lỗi khi gọi API tìm kiếm: " + error.message }]
            };
        }
    }
};
