package com.example.agent.config.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * JVM 内存和会话状态监控
 */
@Component
public class MemoryMetrics {

    private final MemoryMXBean memoryMXBean;

    public MemoryMetrics(MeterRegistry meterRegistry) {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();

        // 堆内存使用
        Gauge.builder("jvm.memory.heap.used", this, m -> m.getHeapUsed())
                .description("JVM堆内存已使用")
                .register(meterRegistry);
        Gauge.builder("jvm.memory.heap.committed", this, m -> m.getHeapCommitted())
                .description("JVM堆内存已分配")
                .register(meterRegistry);
        Gauge.builder("jvm.memory.heap.max", this, m -> m.getHeapMax())
                .description("JVM堆内存最大")
                .register(meterRegistry);

        // 非堆内存使用
        Gauge.builder("jvm.memory.nonheap.used", this, m -> m.getNonHeapUsed())
                .description("JVM非堆内存已使用")
                .register(meterRegistry);

        // 内存使用率
        Gauge.builder("jvm.memory.heap.usage", this, m -> m.getHeapUsagePercent())
                .description("JVM堆内存使用率")
                .register(meterRegistry);
    }

    public long getHeapUsed() {
        return memoryMXBean.getHeapMemoryUsage().getUsed();
    }

    public long getHeapCommitted() {
        return memoryMXBean.getHeapMemoryUsage().getCommitted();
    }

    public long getHeapMax() {
        long max = memoryMXBean.getHeapMemoryUsage().getMax();
        return max < 0 ? memoryMXBean.getHeapMemoryUsage().getCommitted() : max;
    }

    public long getNonHeapUsed() {
        return memoryMXBean.getNonHeapMemoryUsage().getUsed();
    }

    public double getHeapUsagePercent() {
        long max = getHeapMax();
        if (max <= 0) return 0;
        return (double) getHeapUsed() / max * 100;
    }
}
