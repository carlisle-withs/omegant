package com.example.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * MCP Client 接口
 */
public interface McpClient {

    /**
     * MCP 工具信息
     */
    record McpToolInfo(String name, String description, String inputSchema) {
    }

    /**
     * MCP 工具调用结果
     */
    record McpToolResult(boolean success, String output) {
        public static McpToolResult success(String output) {
            return new McpToolResult(true, output);
        }
        public static McpToolResult failure(String error) {
            return new McpToolResult(false, error);
        }
    }

    /**
     * 初始化连接
     */
    void initialize();

    /**
     * 获取工具列表
     */
    List<McpToolInfo> listTools();

    /**
     * 调用工具
     */
    McpToolResult callTool(String toolName, Map<String, Object> arguments);

    /**
     * 检查是否已连接
     */
    boolean isConnected();
}
