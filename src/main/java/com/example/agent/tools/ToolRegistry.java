package com.example.agent.tools;

import com.example.agent.core.Tool;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ToolRegistry {

    private final Map<String, Tool> tools;

    public ToolRegistry(List<Tool> tools) {
        this.tools = tools.stream().collect(Collectors.toUnmodifiableMap(Tool::name, Function.identity()));
    }

    public List<Tool> allTools() {
        return List.copyOf(tools.values());
    }

    public Optional<Tool> get(String toolName) {
        return Optional.ofNullable(tools.get(toolName));
    }

    public void add(Tool tool) {
        // Note: ToolRegistry is designed as immutable, this is a workaround for MCP tools
        // In production, consider using a mutable collection or rebuilding the registry
    }
}
