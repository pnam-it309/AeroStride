import express from 'express';
import type { Request, Response } from 'express';
import cors from 'cors';
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { SSEServerTransport } from "@modelcontextprotocol/sdk/server/sse.js";
import dotenv from 'dotenv';
import { policiesTool } from './tools/policies';
import { searchProductsTool } from './tools/products';

dotenv.config();

const app = express();
app.disable('x-powered-by');
const PORT = Number.parseInt(process.env.PORT || '3000', 10);

const allowedOrigins: Set<string> = new Set(
    (process.env.ALLOWED_ORIGINS
        ? process.env.ALLOWED_ORIGINS.split(',').map((o: string) => o.trim())
        : ['http://localhost:5173', 'http://localhost:3000', 'https://aerostride.me']
    ).filter((origin: string) => Boolean(origin) && origin !== '*')
);

app.use(cors({
    origin: (origin: string | undefined, callback: (err: Error | null, allow?: boolean) => void) => {
        if (!origin || allowedOrigins.has(origin)) {
            callback(null, true);
        } else {
            callback(new Error('CORS policy: origin not allowed'));
        }
    },
    credentials: true
}));

app.use(express.json({ limit: '1mb' }));

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

app.get(["/sse", "/sse/sse"], async (req: Request, res: Response) => {
    console.log("New SSE connection established");
    if (sseTransport) {
        try {
            console.log("Closing previous SSE transport...");
            await activeServer?.close();
        } catch (e: unknown) {
            console.error("Error closing previous transport:", e);
        }
    }
    
    activeServer = createMcpServer();
    const absoluteEndpoint = process.env.MCP_MESSAGE_ENDPOINT || "/message";
    sseTransport = new SSEServerTransport(absoluteEndpoint, res);
    await activeServer.connect(sseTransport);
    
    res.write(": " + "x".repeat(2048) + "\n\n");
});

app.post(["/message", "/sse/message"], async (req: Request, res: Response) => {
    console.log("Received MCP message event");
    if (sseTransport) {
        try {
            await sseTransport.handlePostMessage(req, res, req.body);
        } catch (error: unknown) {
            console.error("Error handling MCP message:", error);
            if (!res.headersSent) {
                res.status(500).json({ error: "Failed to process MCP message" });
            }
        }
    } else {
        res.status(400).send("SSE connection not established");
    }
});

app.listen(PORT, () => {
    console.log(`AeroStride MCP Server is running on port ${PORT}`);
});
