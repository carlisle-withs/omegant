package com.example.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * StreamableHTTP 模式的 MCP Client
 * 支持 JSON-RPC 2.0 的 streamableHttp 模式
 */
@Component
public class StreamableHttpMcpClient {

    private static final Logger log = LoggerFactory.getLogger(StreamableHttpMcpClient.class);

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Map<String, String> defaultHeaders = new ConcurrentHashMap<>();

    private volatile String serverUrl;
    private volatile String sessionId;
    private volatile boolean initialized = false;
    private final AtomicReference<String> endpointId = new AtomicReference<>();

    public StreamableHttpMcpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void initialize(String serverUrl, Map<String, String> headers) {
        this.serverUrl = serverUrl;
        this.defaultHeaders.putAll(headers);

        try {
            log.info("初始化 StreamableHTTP MCP Client: {}", serverUrl);

            // 发送 initialize 请求
            ObjectNode request = objectMapper.createObjectNode();
            request.put("jsonrpc", "2.0");
            request.put("id", "1");
            request.put("method", "initialize");

            ObjectNode params = request.putObject("params");
            params.put("protocolVersion", "2024-11-05");
            params.putObject("capabilities");
            ObjectNode clientInfo = params.putObject("clientInfo");
            clientInfo.put("name", "agent-client");
            clientInfo.put("version", "1.0.0");

            JsonNode response = sendRequest(request);

            if (response != null && response.has("result")) {
                log.info("StreamableHTTP MCP Client 初始化成功");
                initialized = true;

                // 提取 sessionId
                if (response.path("result").has("sessionId")) {
                    this.sessionId = response.path("result").get("sessionId").asText();
                    log.info("获取到 sessionId: {}", sessionId);
                }
            } else if (response != null && response.has("error")) {
                String error = response.path("error").asText();
                log.error("初始化失败: {}", error);
                throw new RuntimeException("初始化失败: " + error);
            }
        } catch (Exception e) {
            log.error("StreamableHTTP MCP Client 初始化失败", e);
            throw new RuntimeException("初始化失败: " + e.getMessage(), e);
        }
    }

    public List<McpClient.McpToolInfo> listTools() {
        if (!initialized) {
            throw new IllegalStateException("Client 未初始化");
        }

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", "2");
        request.put("method", "tools/list");

        if (sessionId != null) {
            ObjectNode params = request.putObject("params");
            params.put("sessionId", sessionId);
        }

        try {
            JsonNode response = sendRequest(request);

            List<McpClient.McpToolInfo> tools = new ArrayList<>();
            if (response != null && response.has("result")) {
                JsonNode toolsNode = response.path("result").path("tools");
                if (toolsNode.isArray()) {
                    for (JsonNode toolNode : toolsNode) {
                        tools.add(new McpClient.McpToolInfo(
                                toolNode.path("name").asText(),
                                toolNode.path("description").asText(),
                                toolNode.path("inputSchema").toString()
                        ));
                    }
                }
            }
            log.info("获取到 {} 个工具", tools.size());
            return tools;
        } catch (Exception e) {
            log.error("获取工具列表失败", e);
            return List.of();
        }
    }

    public McpClient.McpToolResult callTool(String toolName, Map<String, Object> arguments) {
        if (!initialized) {
            throw new IllegalStateException("Client 未初始化");
        }

        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", java.util.UUID.randomUUID().toString());
        request.put("method", "tools/call");

        ObjectNode params = request.putObject("params");
        params.put("name", toolName);
        params.set("arguments", objectMapper.valueToTree(arguments));

        if (sessionId != null) {
            params.put("sessionId", sessionId);
        }

        try {
            log.info("调用 MCP 工具(StreamableHTTP模式): name={}, arguments={}", toolName, arguments);

            JsonNode response = sendRequest(request);

            if (response != null && response.has("error")) {
                String errorMsg = response.path("error").path("message").asText("未知错误");
                return McpClient.McpToolResult.failure(errorMsg);
            }

            if (response != null && response.has("result")) {
                JsonNode contentArray = response.path("result").path("content");
                if (contentArray.isArray() && !contentArray.isEmpty()) {
                    String text = contentArray.get(0).path("text").asText();
                    return McpClient.McpToolResult.success(text);
                }
            }

            return McpClient.McpToolResult.failure("未获取到工具执行结果");
        } catch (Exception e) {
            log.error("调用 MCP 工具失败: name={}", toolName, e);
            return McpClient.McpToolResult.failure("调用失败: " + e.getMessage());
        }
    }

    private JsonNode sendRequest(ObjectNode request) {
        var builder = restClient.post()
                .uri(URI.create(serverUrl))
                .body(request);

        for (Map.Entry<String, String> entry : defaultHeaders.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        return builder.retrieve().body(JsonNode.class);
    }

    public boolean isConnected() {
        return initialized;
    }
}
