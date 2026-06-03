import express from 'express';
import cors from 'cors';
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { SSEServerTransport } from "@modelcontextprotocol/sdk/server/sse.js";
import { z } from "zod";
import dotenv from 'dotenv';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

function createMcpServer() {
    // Initialize the MCP Server
    const server = new McpServer({
        name: "AeroStride MCP Server",
        version: "1.0.0"
    });

    // Register Tool: getStorePolicies
    server.tool(
        "getStorePolicies",
        "Lấy chính sách của cửa hàng AeroStride (bảo hành, đổi trả, v.v.)",
        {},
        async () => {
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
    );

    // Register Tool: searchProducts (Call Backend API)
    server.tool(
        "searchProducts",
        "Tìm kiếm sản phẩm giày thông qua hệ thống Backend AeroStride API",
        {
            query: z.string().describe("Từ khóa tìm kiếm (ví dụ: nike, giày chạy bộ)")
        },
        async ({ query }) => {
            try {
                const backendHost = process.env.BACKEND_HOST || 'backend';
                const backendPort = process.env.BE_INTERNAL_PORT || process.env.BACKEND_PORT || '8080';
                const apiPrefix = process.env.APP_API_PREFIX || '/api/v1';
                const baseUrl = `http://${backendHost}:${backendPort}${apiPrefix}`;
                const apiUrl = `${baseUrl}/customer/san-pham/hien-thi?keyword=${encodeURIComponent(query)}&size=10`;
                
                const response = await fetch(apiUrl);
                if (!response.ok) {
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
                products.forEach((product: any) => {
                    const brand = product.tenThuongHieu ? `[${product.tenThuongHieu}]` : '';
                    const price = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.giaBanNhoNhat || 0);
                    const discount = product.giaTriGiamGia ? ` (Giảm giá ${product.giaTriGiamGia}%)` : '';
                    resultText += `- ${brand} ${product.tenSanPham} - Giá từ: ${price}${discount} - Đã bán: ${product.soLuongDaBan || 0}\n`;
                });

                return {
                    content: [{ type: "text", text: resultText }]
                };
            } catch (error) {
                console.error("API search error:", error);
                return {
                    content: [{ type: "text", text: "Lỗi khi gọi API tìm kiếm: " + (error as Error).message }]
                };
            }
        }
    );
    
    return server;
}

// We need to keep a reference to the active SSE transport and active server
let sseTransport: SSEServerTransport | null = null;
let activeServer: McpServer | null = null;

// Endpoint 1: SSE connection for Spring AI client
app.get(["/sse", "/sse/sse"], async (req, res) => {
    console.log("New SSE connection established");
    if (sseTransport) {
        try {
            console.log("Closing previous SSE transport...");
            await activeServer?.close();
        } catch (e) {
            console.error("Error closing previous transport:", e);
        }
    }
    
    activeServer = createMcpServer();
    const absoluteEndpoint = `http://aerostride-mcp-server:3000/message`;
    sseTransport = new SSEServerTransport(absoluteEndpoint, res);
    await activeServer.connect(sseTransport);
    
    // Force flush the network buffer to prevent Spring WebClient from timing out
    res.write(": " + "x".repeat(2048) + "\n\n");
});

// Endpoint 2: Message reception for JSON-RPC
app.post("/message", async (req, res) => {
    console.log("Received MCP message:", req.body);
    if (sseTransport) {
        try {
            await sseTransport.handlePostMessage(req, res, req.body);
        } catch (error) {
            console.error("Error handling MCP message:", error);
        }
    } else {
        res.status(400).send("SSE connection not established");
    }
});

app.listen(PORT, () => {
    console.log(`AeroStride MCP Server is running on http://localhost:${PORT}`);
});
