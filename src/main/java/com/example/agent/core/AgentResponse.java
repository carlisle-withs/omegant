package com.example.agent.core;

import java.util.List;

public record AgentResponse(
        String content,
        String finishReason,
        List<ToolCall> toolCalls
) {
}
