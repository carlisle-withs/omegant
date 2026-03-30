package com.example.agent.mcp;

/**
 * MCP Client 配置
 */
public class McpClientProperties {

    private String serverUrl;
    private int connectTimeout = 10000;
    private int readTimeout = 60000;

    public McpClientProperties() {
    }

    public McpClientProperties(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String serverUrl;
        private int connectTimeout = 10000;
        private int readTimeout = 60000;

        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public McpClientProperties build() {
            McpClientProperties props = new McpClientProperties();
            props.setServerUrl(serverUrl);
            props.setConnectTimeout(connectTimeout);
            props.setReadTimeout(readTimeout);
            return props;
        }
    }
}
