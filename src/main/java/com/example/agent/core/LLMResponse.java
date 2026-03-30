package com.example.agent.core;

import java.util.List;

public record LLMResponse(
        String content,
        List<ToolCall> toolCalls
) {
}
