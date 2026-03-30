package com.example.agent.gateway;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class InMemoryRateLimiter {

    private final int maxRequests;
    private final Duration window;
    private final Map<String, Deque<Instant>> requestHistory = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    public boolean allow(String key) {
        Deque<Instant> timestamps = requestHistory.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
        Instant threshold = Instant.now().minus(window);

        while (true) {
            Instant first = timestamps.peekFirst();
            if (first == null || !first.isBefore(threshold)) {
                break;
            }
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            return false;
        }

        timestamps.addLast(Instant.now());
        return true;
    }
}
