package com.example.agent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Client 工厂 - 根据配置创建对应的 Client 实例
 */
@Component
public class McpClientFactory {

    private static final Logger log = LoggerFactory.getLogger(McpClientFactory.class);

    private final ObjectMapper objectMapper;
    private final Map<String, McpClientWrapper> clientCache = new ConcurrentHashMap<>();

    public McpClientFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized McpClientWrapper getOrCreateClient(McpServerConfig config) {
        if (clientCache.containsKey(config.getName())) {
            log.info("使用已缓存的 MCP Client: {}", config.getName());
            return clientCache.get(config.getName());
        }

        log.info("创建新的 MCP Client: name={}, type={}", config.getName(), config.getType());
        McpClientWrapper wrapper;

        if (config.isSseMode()) {
            wrapper = createSseClient(config);
        } else if (config.isStreamableHttpMode()) {
            wrapper = createStreamableHttpClient(config);
        } else {
            wrapper = createHttpClient(config);
        }

        clientCache.put(config.getName(), wrapper);
        return wrapper;
    }

    private McpClientWrapper createHttpClient(McpServerConfig config) {
        McpClientProperties properties = McpClientProperties.builder()
                .serverUrl(config.getServerUrl())
                .build();

        HttpMcpClient client = new HttpMcpClient(properties, objectMapper);
        client.initialize();

        List<McpClient.McpToolInfo> tools = client.listTools();
        log.info("HTTP MCP Client 初始化成功, {} 个工具", tools.size());

        return new McpClientWrapper(client, null, null, tools);
    }

    private McpClientWrapper createSseClient(McpServerConfig config) {
        SseMcpClient client = new SseMcpClient(objectMapper);
        client.initialize(config.getSseEndpoint(), config.getHeaders());

        List<McpClient.McpToolInfo> tools = client.listTools();
        log.info("SSE MCP Client 初始化成功, {} 个工具", tools.size());

        return new McpClientWrapper(null, client, null, tools);
    }

    private McpClientWrapper createStreamableHttpClient(McpServerConfig config) {
        StreamableHttpMcpClient client = new StreamableHttpMcpClient(objectMapper);
        client.initialize(config.getServerUrl(), config.getHeaders());

        List<McpClient.McpToolInfo> tools = client.listTools();
        log.info("StreamableHTTP MCP Client 初始化成功, {} 个工具", tools.size());

        return new McpClientWrapper(null, null, client, tools);
    }

    public void clearCache() {
        clientCache.clear();
    }

    /**
     * MCP Client 包装器 - 统一不同 Client 的调用方式
     */
    public static class McpClientWrapper {
        private final HttpMcpClient httpClient;
        private final SseMcpClient sseClient;
        private final StreamableHttpMcpClient streamableHttpClient;
        private final List<McpClient.McpToolInfo> tools;

        public McpClientWrapper(
                HttpMcpClient httpClient,
                SseMcpClient sseClient,
                StreamableHttpMcpClient streamableHttpClient,
                List<McpClient.McpToolInfo> tools
        ) {
            this.httpClient = httpClient;
            this.sseClient = sseClient;
            this.streamableHttpClient = streamableHttpClient;
            this.tools = tools;
        }

        public McpClient.McpToolResult callTool(String toolName, Map<String, Object> arguments) {
            if (httpClient != null) {
                return httpClient.callTool(toolName, arguments);
            } else if (sseClient != null) {
                return sseClient.callTool(toolName, arguments);
            } else if (streamableHttpClient != null) {
                return streamableHttpClient.callTool(toolName, arguments);
            }
            throw new IllegalStateException("No MCP Client available");
        }

        public List<McpClient.McpToolInfo> getTools() {
            return tools;
        }

        public boolean isHttpClient() {
            return httpClient != null;
        }

        public boolean isSseClient() {
            return sseClient != null;
        }

        public boolean isStreamableHttpClient() {
            return streamableHttpClient != null;
        }
    }
}
