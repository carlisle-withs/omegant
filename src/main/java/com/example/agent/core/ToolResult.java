package com.example.agent.core;

public record ToolResult(
        boolean success,
        String output,
        String error
) {
    public static ToolResult success(String output) {
        return new ToolResult(true, output, null);
    }

    public static ToolResult failure(String error) {
        return new ToolResult(false, null, error);
    }
}
