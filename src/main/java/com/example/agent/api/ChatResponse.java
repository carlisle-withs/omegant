package com.example.agent.api;

import com.example.agent.core.ToolCall;

import java.util.List;

public record ChatResponse(
        String requestId,
        String agentId,
        String content,
        String finishReason,
        List<ToolCall> toolCalls
) {
}
