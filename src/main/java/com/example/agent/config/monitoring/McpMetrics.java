package com.example.agent.config.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP 服务连接状态监控
 */
@Component
public class McpMetrics {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger connectedServers;
    private final AtomicInteger activeConnections;
    private final Counter connectionErrors;
    private final Counter messagesReceived;
    private final Counter messagesSent;

    public McpMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.connectedServers = new AtomicInteger(0);
        this.activeConnections = new AtomicInteger(0);
        this.connectionErrors = Counter.builder("mcp.connection.errors")
                .description("MCP连接错误次数")
                .register(meterRegistry);
        this.messagesReceived = Counter.builder("mcp.messages.received")
                .description("收到的消息数")
                .register(meterRegistry);
        this.messagesSent = Counter.builder("mcp.messages.sent")
                .description("发送的消息数")
                .register(meterRegistry);

        Gauge.builder("mcp.servers.connected", connectedServers, AtomicInteger::get)
                .description("已连接的MCP服务器数量")
                .register(meterRegistry);
        Gauge.builder("mcp.connections.active", activeConnections, AtomicInteger::get)
                .description("活跃的MCP连接数量")
                .register(meterRegistry);
    }

    public void serverConnected() {
        connectedServers.incrementAndGet();
        activeConnections.incrementAndGet();
    }

    public void serverDisconnected() {
        connectedServers.decrementAndGet();
        activeConnections.decrementAndGet();
    }

    public void connectionError() {
        connectionErrors.increment();
        activeConnections.decrementAndGet();
    }

    public void messageReceived() {
        messagesReceived.increment();
    }

    public void messageSent() {
        messagesSent.increment();
    }

    public int getConnectedServers() {
        return connectedServers.get();
    }
}
