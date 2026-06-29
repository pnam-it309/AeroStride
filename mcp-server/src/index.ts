import express from 'express';
import cors from 'cors';
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { SSEServerTransport } from "@modelcontextprotocol/sdk/server/sse.js";
import dotenv from 'dotenv';
import { policiesTool } from './tools/policies';
import { searchProductsTool } from './tools/products';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

function createMcpServer() {
    console.log("Initializing AeroStride MCP Server...");
    const server = new McpServer({
        name: "AeroStride MCP Server",
        version: "1.0.0"
    });

    server.tool(
        policiesTool.name,
        policiesTool.description,
        policiesTool.parameters,
        policiesTool.handler
    );

    server.tool(
        searchProductsTool.name,
        searchProductsTool.description,
        searchProductsTool.parameters,
        searchProductsTool.handler
    );
    
    return server;
}

let sseTransport: SSEServerTransport | null = null;
let activeServer: McpServer | null = null;

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
    
    res.write(": " + "x".repeat(2048) + "\n\n");
});

app.post(["/message", "/sse/message"], async (req, res) => {
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
