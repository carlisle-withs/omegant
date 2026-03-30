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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SSE 模式的 MCP Client
 * 通过 SSE 端点接收服务端推送，通过 HTTP POST 调用工具
 */
@Component
public class SseMcpClient {

    private static final Logger log = LoggerFactory.getLogger(SseMcpClient.class);

    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;

    private volatile boolean initialized = false;
    private volatile String sseEndpoint;
    private volatile String actualMcpEndpoint;
    private volatile String baseUrl;
    private volatile Map<String, String> headers = new HashMap<>();

    private CompletableFuture<Void> sseConnectionFuture;
    private final CountDownLatch initLatch = new CountDownLatch(1);

    public SseMcpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    public synchronized void initialize(String sseEndpoint) {
        initialize(sseEndpoint, new HashMap<>());
    }

    public synchronized void initialize(String sseEndpoint, Map<String, String> headers) {
        if (initialized && this.sseEndpoint != null && this.sseEndpoint.equals(sseEndpoint)) {
            log.info("SSE MCP 已连接到: {}", sseEndpoint);
            return;
        }

        this.sseEndpoint = sseEndpoint;
        this.headers = new HashMap<>(headers);

        try {
            URL url = new URL(sseEndpoint);
            this.baseUrl = url.getProtocol() + "://" + url.getHost();
            if (url.getPort() != -1) {
                this.baseUrl += ":" + url.getPort();
            }
        } catch (Exception e) {
            log.error("解析 baseUrl 失败", e);
            throw new RuntimeException("解析 baseUrl 失败: " + e.getMessage(), e);
        }

        log.info("初始化 SSE MCP 连接: sseEndpoint={}, baseUrl={}, headers={}", sseEndpoint, baseUrl, headers.keySet());

        try {
            startSseConnection();
            boolean initSuccess = initLatch.await(30, TimeUnit.SECONDS);
            if (!initSuccess) {
                throw new RuntimeException("SSE 初始化超时，未收到 endpoint 事件");
            }
            initialized = true;
            log.info("SSE MCP 连接初始化成功, actualMcpEndpoint={}", actualMcpEndpoint);
        } catch (Exception e) {
            log.error("SSE MCP 连接初始化失败", e);
            throw new RuntimeException("SSE MCP 连接初始化失败: " + e.getMessage(), e);
        }
    }

    private void startSseConnection() {
        sseConnectionFuture = CompletableFuture.runAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    log.info("连接 SSE 端点: {}", sseEndpoint);
                    connectSse();
                } catch (Exception e) {
                    log.error("SSE 连接异常，5秒后重连", e);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, scheduler);
    }

    private void connectSse() throws Exception {
        URL url = new URL(sseEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setReadTimeout(300000); // 5分钟超时

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }

        conn.setDoInput(true);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            StringBuilder eventBuilder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    String eventData = eventBuilder.toString().trim();
                    if (!eventData.isEmpty()) {
                        handleSseEvent(eventData);
                    }
                    eventBuilder.setLength(0);
                } else if (line.startsWith("data:")) {
                    eventBuilder.append(line.substring(5)).append("\n");
                }
            }
        } finally {
            conn.disconnect();
        }
    }

    private void handleSseEvent(String event) {
        try {
            log.debug("收到 SSE 事件: {}", event);

            if (event.startsWith("/")) {
                String endpointPath = event;
                this.actualMcpEndpoint = baseUrl + endpointPath;
                log.info("获取到实际 MCP 端点: {}", actualMcpEndpoint);
                initLatch.countDown();
                return;
            }

            JsonNode data = objectMapper.readTree(event);

            if (data.has("method") && "ping".equals(data.get("method").asText())) {
                log.debug("收到 ping 事件");
            }

            if (data.has("method") && "initialized".equals(data.get("method").asText())) {
                log.info("收到服务端初始化通知");
            }

        } catch (Exception e) {
            log.error("处理 SSE 事件异常: {}", event, e);
        }
    }

    public List<McpClient.McpToolInfo> listTools() {
        ensureInitialized();

        String id = java.util.UUID.randomUUID().toString();
        ObjectNode request = createRequest(id, "tools/list");

        try {
            ObjectNode response = httpPost(request);

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
        ensureInitialized();

        String id = java.util.UUID.randomUUID().toString();
        ObjectNode request = createRequest(id, "tools/call");

        ObjectNode params = request.putObject("params");
        params.put("name", toolName);
        params.set("arguments", objectMapper.valueToTree(arguments));

        try {
            log.info("调用 MCP 工具(SSE模式): name={}, arguments={}", toolName, arguments);
            ObjectNode response = httpPost(request);

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

    private ObjectNode httpPost(ObjectNode request) {
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(10));
        requestFactory.setReadTimeout(java.time.Duration.ofSeconds(30));

        RestClient restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();

        var builder = restClient.post()
                .uri(URI.create(actualMcpEndpoint))
                .body(request);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        return builder
                .retrieve()
                .body(ObjectNode.class);
    }

    private ObjectNode createRequest(String id, String method) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        return request;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("SSE MCP Client 未初始化，请先调用 initialize(sseEndpoint)");
        }
    }

    public boolean isConnected() {
        return initialized;
    }

    public String getSseEndpoint() {
        return sseEndpoint;
    }

    public String getActualMcpEndpoint() {
        return actualMcpEndpoint;
    }
}
