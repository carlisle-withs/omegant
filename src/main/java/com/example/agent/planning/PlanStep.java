package com.example.agent.planning;

import com.example.agent.core.ToolCall;

import java.util.Map;

/**
 * 计划步骤
 */
public record PlanStep(
        int index,
        String description,
        String toolName,
        Map<String, Object> parameters,
        PlanStepStatus status
) {
    public PlanStep(int index, String description, String toolName, Map<String, Object> parameters) {
        this(index, description, toolName, parameters, PlanStepStatus.PENDING);
    }

    public PlanStep withStatus(PlanStepStatus status) {
        return new PlanStep(index, description, toolName, parameters, status);
    }

    public ToolCall toToolCall(String id) {
        return new ToolCall(id, toolName, parameters);
    }
}
