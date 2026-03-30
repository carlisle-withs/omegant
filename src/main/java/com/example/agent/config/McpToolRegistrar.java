package com.example.agent.config;

import com.example.agent.core.Tool;
import com.example.agent.mcp.McpClient;
import com.example.agent.mcp.McpClientFactory;
import com.example.agent.mcp.McpServerConfig;
import com.example.agent.mcp.McpServersProperties;
import com.example.agent.tools.ToolRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具注册器 - 将多个 MCP Server 的工具注册到 ToolRegistry
 */
@Component
public class McpToolRegistrar {

    private static final Logger log = LoggerFactory.getLogger(McpToolRegistrar.class);

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private McpServersProperties mcpServersProperties;

    @Autowired
    private McpClientFactory mcpClientFactory;

    @PostConstruct
    public void registerMcpTools() {
        if (!mcpServersProperties.isEnabled()) {
            log.info("MCP 功能已禁用");
            return;
        }

        List<McpServerConfig> servers = mcpServersProperties.getEnabledServers();
        if (servers.isEmpty()) {
            log.info("没有已启用的 MCP Server");
            return;
        }

        log.info("开始注册 MCP 工具，共 {} 个 Server", servers.size());

        int totalTools = 0;
        for (McpServerConfig serverConfig : servers) {
            try {
                int registered = registerServerTools(serverConfig);
                totalTools += registered;
            } catch (Exception e) {
                log.error("注册 MCP Server [{}] 工具失败", serverConfig.getName(), e);
            }
        }

        log.info("MCP 工具注册完成，共注册 {} 个工具", totalTools);
    }

    private int registerServerTools(McpServerConfig serverConfig) {
        log.info("连接 MCP Server: name={}, type={}", serverConfig.getName(), serverConfig.getType());

        McpClientFactory.McpClientWrapper clientWrapper = mcpClientFactory.getOrCreateClient(serverConfig);
        List<McpClient.McpToolInfo> tools = clientWrapper.getTools();

        if (tools.isEmpty()) {
            log.warn("MCP Server [{}] 未返回任何工具", serverConfig.getName());
            return 0;
        }

        int registered = 0;
        for (McpClient.McpToolInfo toolInfo : tools) {
            Tool tool = new McpToolAdapter(clientWrapper, toolInfo, serverConfig.getName());
            toolRegistry.add(tool);
            log.debug("注册 MCP 工具: {} -> {}", toolInfo.name(), tool.name());
            registered++;
        }

        log.info("MCP Server [{}] 注册了 {} 个工具", serverConfig.getName(), registered);
        return registered;
    }

    /**
     * MCP 工具适配器 - 将 MCP 工具适配为 Tool 接口
     */
    private static class McpToolAdapter implements Tool {
        private final McpClientFactory.McpClientWrapper clientWrapper;
        private final McpClient.McpToolInfo toolInfo;
        private final String serverName;

        public McpToolAdapter(McpClientFactory.McpClientWrapper clientWrapper,
                              McpClient.McpToolInfo toolInfo,
                              String serverName) {
            this.clientWrapper = clientWrapper;
            this.toolInfo = toolInfo;
            this.serverName = serverName;
        }

        @Override
        public String name() {
            return "mcp_" + serverName + "_" + toolInfo.name();
        }

        @Override
        public String description() {
            return "[MCP:" + serverName + "] " + toolInfo.description();
        }

        @Override
        public String parameterSchema() {
            return toolInfo.inputSchema();
        }

        @Override
        public com.example.agent.core.ToolResult execute(Map<String, Object> parameters) {
            McpClient.McpToolResult result = clientWrapper.callTool(toolInfo.name(), parameters);
            if (result.success()) {
                return com.example.agent.core.ToolResult.success(result.output());
            } else {
                return com.example.agent.core.ToolResult.failure(result.output());
            }
        }
    }
}
