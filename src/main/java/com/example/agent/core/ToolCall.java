package com.example.agent.core;

import java.util.Map;

public record ToolCall(
        String id,
        String name,
        Map<String, Object> parameters
) {
    public ToolCall(String name, Map<String, Object> parameters) {
        this(null, name, parameters);
    }
}
