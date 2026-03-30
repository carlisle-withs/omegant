package com.example.agent.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Servers 配置属性
 */
@ConfigurationProperties(prefix = "agent.mcp")
public class McpServersProperties {

    private boolean enabled = true;
    private List<McpServerConfig> servers = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<McpServerConfig> getServers() {
        return servers;
    }

    public void setServers(List<McpServerConfig> servers) {
        this.servers = servers;
    }

    public List<McpServerConfig> getEnabledServers() {
        return servers.stream()
                .filter(McpServerConfig::isEnabled)
                .toList();
    }
}
