package com.example.agent.core;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record Message(
        MessageRole role,
        String content,
        String name,
        Map<String, Object> metadata,
        Instant createdAt
) {
    public static Message user(String content) {
        return new Message(MessageRole.USER, content, null, Map.of(), Instant.now());
    }

    public static Message assistant(String content) {
        return new Message(MessageRole.ASSISTANT, content, null, Map.of(), Instant.now());
    }

    public static Message assistantToolCalls(String content, List<ToolCall> toolCalls) {
        return new Message(MessageRole.ASSISTANT, content, null, Map.of("toolCalls", toolCalls), Instant.now());
    }

    public static Message tool(String toolName, String content, Map<String, Object> metadata) {
        return new Message(MessageRole.TOOL, content, toolName, metadata, Instant.now());
    }
}
