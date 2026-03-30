package com.example.agent.core;

/**
 * LLM 流式响应的数据块
 */
public record ChatChunk(
        String delta,
        ToolCall toolCall,
        Boolean complete,
        String finishReason
) {
    public static ChatChunk content(String delta) {
        return new ChatChunk(delta, null, null, null);
    }

    public static ChatChunk toolCall(ToolCall toolCall) {
        return new ChatChunk(null, toolCall, null, null);
    }

    public static ChatChunk done(String finishReason) {
        return new ChatChunk(null, null, true, finishReason);
    }
}
