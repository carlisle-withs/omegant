package com.example.agent.mcp;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Map;

/**
 * MCP Server 配置
 */
public class McpServerConfig {

    private String name;
    private String type;  // http, sse, streamableHttp
    private boolean enabled = true;
    private String serverUrl;
    private String sseEndpoint;
    private Map<String, String> headers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getSseEndpoint() {
        return sseEndpoint;
    }

    public void setSseEndpoint(String sseEndpoint) {
        this.sseEndpoint = sseEndpoint;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public boolean isSseMode() {
        return "sse".equalsIgnoreCase(type);
    }

    public boolean isStreamableHttpMode() {
        return "streamableHttp".equalsIgnoreCase(type);
    }
}
