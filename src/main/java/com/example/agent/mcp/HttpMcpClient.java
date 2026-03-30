package com.example.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP 模式的 MCP Client
 */
public class HttpMcpClient implements McpClient {

    private static final Logger log = LoggerFactory.getLogger(HttpMcpClient.class);

    private final McpClientProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Map<String, String> defaultHeaders = new ConcurrentHashMap<>();

    private volatile boolean initialized = false;
    private List<McpToolInfo> cachedTools = new ArrayList<>();

    public HttpMcpClient(McpClientProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void initialize() {
        if (initialized) {
            log.info("HTTP MCP Client 已初始化");
            return;
        }

        try {
            log.info("初始化 HTTP MCP Client: {}", properties.getServerUrl());

            // 发送 initialize 请求
            ObjectNode initRequest = objectMapper.createObjectNode();
            initRequest.put("jsonrpc", "2.0");
            initRequest.put("id", "1");
            initRequest.put("method", "initialize");

            ObjectNode params = initRequest.putObject("params");
            params.put("protocolVersion", "2024-11-05");
            params.put("capabilities", "{}");
            params.put("clientInfo", "{}");

            // 不需要等待响应，只需要连接成功即可
            try {
                restClient.post()
                        .uri(URI.create(properties.getServerUrl()))
                        .body(initRequest)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (Exception e) {
                log.warn("initialize 请求可能失败: {}", e.getMessage());
            }

            initialized = true;
            log.info("HTTP MCP Client 初始化成功");
        } catch (Exception e) {
            log.error("HTTP MCP Client 初始化失败", e);
            throw new RuntimeException("HTTP MCP Client 初始化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<McpToolInfo> listTools() {
        if (!initialized) {
            initialize();
        }

        if (!cachedTools.isEmpty()) {
            return cachedTools;
        }

        try {
            ObjectNode request = createRequest("tools/list");

            JsonNode response = restClient.post()
                    .uri(URI.create(properties.getServerUrl()))
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            List<McpToolInfo> tools = new ArrayList<>();
            if (response != null && response.has("result")) {
                JsonNode toolsNode = response.path("result").path("tools");
                if (toolsNode.isArray()) {
                    for (JsonNode toolNode : toolsNode) {
                        tools.add(new McpToolInfo(
                                toolNode.path("name").asText(),
                                toolNode.path("description").asText(),
                                toolNode.path("inputSchema").toString()
                        ));
                    }
                }
            }

            this.cachedTools = tools;
            log.info("获取到 {} 个工具", tools.size());
            return tools;
        } catch (Exception e) {
            log.error("获取工具列表失败", e);
            return List.of();
        }
    }

    @Override
    public McpToolResult callTool(String toolName, Map<String, Object> arguments) {
        if (!initialized) {
            initialize();
        }

        String id = java.util.UUID.randomUUID().toString();
        ObjectNode request = createRequest("tools/call");

        ObjectNode params = request.putObject("params");
        params.put("name", toolName);
        params.set("arguments", objectMapper.valueToTree(arguments));

        try {
            log.info("调用 MCP 工具(HTTP模式): name={}, arguments={}", toolName, arguments);

            JsonNode response = restClient.post()
                    .uri(URI.create(properties.getServerUrl()))
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("error")) {
                String errorMsg = response.path("error").path("message").asText("未知错误");
                return McpToolResult.failure(errorMsg);
            }

            if (response != null && response.has("result")) {
                JsonNode contentArray = response.path("result").path("content");
                if (contentArray.isArray() && !contentArray.isEmpty()) {
                    String text = contentArray.get(0).path("text").asText();
                    return McpToolResult.success(text);
                }
            }

            return McpToolResult.failure("未获取到工具执行结果");
        } catch (Exception e) {
            log.error("调用 MCP 工具失败: name={}", toolName, e);
            return McpToolResult.failure("调用失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return initialized;
    }

    private ObjectNode createRequest(String method) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", java.util.UUID.randomUUID().toString());
        request.put("method", method);
        return request;
    }
}
