package com.example.agent.config.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 运行时指标监控
 */
@Component
public class AgentMetrics {

    private final MeterRegistry meterRegistry;

    // 请求计数
    private final Counter totalRequests;
    private final Counter successfulRequests;
    private final Counter failedRequests;
    private final Counter toolCallsTotal;

    // 意图类型计数
    private final Counter intentChat;
    private final Counter intentToolCall;
    private final Counter intentPlanning;
    private final Counter intentQuery;

    // 运行时长
    private final Timer requestDuration;
    private final Timer toolExecutionDuration;

    // 当前状态
    private final AtomicInteger activeRequests;
    private final AtomicInteger planningInProgress;

    public AgentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 请求计数
        this.totalRequests = Counter.builder("agent.requests.total")
                .description("总请求数")
                .register(meterRegistry);
        this.successfulRequests = Counter.builder("agent.requests.success")
                .description("成功请求数")
                .register(meterRegistry);
        this.failedRequests = Counter.builder("agent.requests.failed")
                .description("失败请求数")
                .register(meterRegistry);
        this.toolCallsTotal = Counter.builder("agent.tool.calls.total")
                .description("工具调用总次数")
                .register(meterRegistry);

        // 意图类型计数
        this.intentChat = Counter.builder("agent.intent.chat")
                .description("对话意图次数")
                .register(meterRegistry);
        this.intentToolCall = Counter.builder("agent.intent.tool_call")
                .description("工具调用意图次数")
                .register(meterRegistry);
        this.intentPlanning = Counter.builder("agent.intent.planning")
                .description("规划意图次数")
                .register(meterRegistry);
        this.intentQuery = Counter.builder("agent.intent.query")
                .description("查询意图次数")
                .register(meterRegistry);

        // 运行时长
        this.requestDuration = Timer.builder("agent.request.duration")
                .description("请求处理时长")
                .register(meterRegistry);
        this.toolExecutionDuration = Timer.builder("agent.tool.execution.duration")
                .description("工具执行时长")
                .register(meterRegistry);

        // 当前状态
        this.activeRequests = new AtomicInteger(0);
        this.planningInProgress = new AtomicInteger(0);

        // 注册活跃请求数
        meterRegistry.gauge("agent.requests.active", activeRequests);
        meterRegistry.gauge("agent.planning.active", planningInProgress);
    }

    public void recordRequest() {
        totalRequests.increment();
        activeRequests.incrementAndGet();
    }

    public void recordSuccess() {
        successfulRequests.increment();
        activeRequests.decrementAndGet();
    }

    public void recordFailure() {
        failedRequests.increment();
        activeRequests.decrementAndGet();
    }

    public void recordToolCall() {
        toolCallsTotal.increment();
    }

    public void recordIntent(String intentType) {
        switch (intentType.toUpperCase()) {
            case "CHAT" -> intentChat.increment();
            case "TOOL_CALL" -> intentToolCall.increment();
            case "PLANNING" -> intentPlanning.increment();
            case "QUERY" -> intentQuery.increment();
        }
    }

    public void recordRequestDuration(long durationMs) {
        requestDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordToolExecutionDuration(long durationMs) {
        toolExecutionDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void startPlanning() {
        planningInProgress.incrementAndGet();
    }

    public void endPlanning() {
        planningInProgress.decrementAndGet();
    }
}
