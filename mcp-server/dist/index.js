"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const mcp_js_1 = require("@modelcontextprotocol/sdk/server/mcp.js");
const sse_js_1 = require("@modelcontextprotocol/sdk/server/sse.js");
const dotenv_1 = __importDefault(require("dotenv"));
const policies_1 = require("./tools/policies");
const products_1 = require("./tools/products");
dotenv_1.default.config();
const app = (0, express_1.default)();
app.disable('x-powered-by');
const PORT = Number.parseInt(process.env.PORT || '3000', 10);
const allowedOrigins = new Set((process.env.ALLOWED_ORIGINS
    ? process.env.ALLOWED_ORIGINS.split(',').map((o) => o.trim())
    : ['http://localhost:5173', 'http://localhost:3000', 'https://aerostride.me']).filter((origin) => Boolean(origin) && origin !== '*'));
app.use((0, cors_1.default)({
    origin: (origin, callback) => {
        if (!origin || allowedOrigins.has(origin)) {
            callback(null, true);
        }
        else {
            callback(new Error('CORS policy: origin not allowed'));
        }
    },
    credentials: true
}));
app.use(express_1.default.json({ limit: '1mb' }));
function createMcpServer() {
    console.log("Initializing AeroStride MCP Server...");
    const server = new mcp_js_1.McpServer({
        name: "AeroStride MCP Server",
        version: "1.0.0"
    });
    server.tool(policies_1.policiesTool.name, policies_1.policiesTool.description, policies_1.policiesTool.parameters, policies_1.policiesTool.handler);
    server.tool(products_1.searchProductsTool.name, products_1.searchProductsTool.description, products_1.searchProductsTool.parameters, products_1.searchProductsTool.handler);
    return server;
}
let sseTransport = null;
let activeServer = null;
app.get(["/sse", "/sse/sse"], async (req, res) => {
    console.log("New SSE connection established");
    if (sseTransport) {
        try {
            console.log("Closing previous SSE transport...");
            await activeServer?.close();
        }
        catch (e) {
            console.error("Error closing previous transport:", e);
        }
    }
    activeServer = createMcpServer();
    const absoluteEndpoint = process.env.MCP_MESSAGE_ENDPOINT || "/message";
    sseTransport = new sse_js_1.SSEServerTransport(absoluteEndpoint, res);
    await activeServer.connect(sseTransport);
    res.write(": " + "x".repeat(2048) + "\n\n");
});
app.post(["/message", "/sse/message"], async (req, res) => {
    console.log("Received MCP message event");
    if (sseTransport) {
        try {
            await sseTransport.handlePostMessage(req, res, req.body);
        }
        catch (error) {
            console.error("Error handling MCP message:", error);
            if (!res.headersSent) {
                res.status(500).json({ error: "Failed to process MCP message" });
            }
        }
    }
    else {
        res.status(400).send("SSE connection not established");
    }
});
app.listen(PORT, () => {
    console.log(`AeroStride MCP Server is running on port ${PORT}`);
});
