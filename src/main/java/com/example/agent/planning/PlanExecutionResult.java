package com.example.agent.planning;

import com.example.agent.core.ToolCall;

import java.util.List;

/**
 * 计划执行结果
 */
public record PlanExecutionResult(
        PlanStatus status,
        String finalResponse,
        List<ToolCall> executedToolCalls
) {
    public boolean isSuccess() {
        return status == PlanStatus.SUCCESS;
    }

    public boolean isFailure() {
        return status == PlanStatus.FAILURE;
    }

    public boolean requiresReplan() {
        return status == PlanStatus.REQUIRES_REPLAN;
    }

    public static PlanExecutionResult success(String response, List<ToolCall> toolCalls) {
        return new PlanExecutionResult(PlanStatus.SUCCESS, response, toolCalls);
    }

    public static PlanExecutionResult failure(String response, List<ToolCall> toolCalls) {
        return new PlanExecutionResult(PlanStatus.FAILURE, response, toolCalls);
    }

    public static PlanExecutionResult inProgress(String response, List<ToolCall> toolCalls) {
        return new PlanExecutionResult(PlanStatus.IN_PROGRESS, response, toolCalls);
    }

    public static PlanExecutionResult requiresReplan(String response, List<ToolCall> toolCalls) {
        return new PlanExecutionResult(PlanStatus.REQUIRES_REPLAN, response, toolCalls);
    }
}
