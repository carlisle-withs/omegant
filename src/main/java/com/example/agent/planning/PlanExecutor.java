package com.example.agent.planning;

import com.example.agent.core.*;
import com.example.agent.router.SessionManager;
import com.example.agent.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 计划执行器
 */
@Component
public class PlanExecutor {

    private static final Logger log = LoggerFactory.getLogger(PlanExecutor.class);

    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final SessionManager sessionManager;

    public PlanExecutor(LLMClient llmClient, ToolRegistry toolRegistry, SessionManager sessionManager) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.sessionManager = sessionManager;
    }

    public PlanExecutionResult executePlan(Plan plan, Memory memory) {
        List<ToolCall> executedToolCalls = new ArrayList<>();
        StringBuilder responseBuilder = new StringBuilder();

        log.info("开始执行计划，共 {} 个步骤", plan.totalSteps());

        for (int i = plan.currentStep(); i < plan.totalSteps(); i++) {
            PlanStep step = plan.steps().get(i);
            log.info("执行步骤 {}: {}", step.index(), step.description());

            ToolCall toolCall = step.toToolCall("plan_" + step.index());
            executedToolCalls.add(toolCall);

            ToolResult result = executeTool(step.toolName(), step.parameters());
            String output = formatResult(step.toolName(), result);

            // 将工具调用和结果加入记忆
            memory.add(Message.assistant(step.description() + " -> " + output));

            if (result.success()) {
                responseBuilder.append("步骤").append(step.index()).append(": ").append(step.description()).append(" ✓\n");
            } else {
                responseBuilder.append("步骤").append(step.index()).append(": ").append(step.description()).append(" ✗\n");
                responseBuilder.append("错误: ").append(result.error()).append("\n");
                return PlanExecutionResult.failure(responseBuilder.toString(), executedToolCalls);
            }
        }

        responseBuilder.append("\n所有步骤执行完成！");

        // 使用 LLM 生成最终回复
        String finalResponse = generateFinalResponse(plan.goal(), executedToolCalls, memory);

        return PlanExecutionResult.success(finalResponse, executedToolCalls);
    }

    private ToolResult executeTool(String toolName, Map<String, Object> parameters) {
        return toolRegistry.get(toolName)
                .map(tool -> {
                    try {
                        return tool.execute(parameters);
                    } catch (Exception e) {
                        return ToolResult.failure(e.getMessage());
                    }
                })
                .orElseGet(() -> ToolResult.failure("未找到工具: " + toolName));
    }

    private String formatResult(String toolName, ToolResult result) {
        if (result.success()) {
            return result.output();
        }
        return "错误: " + result.error();
    }

    private String generateFinalResponse(String goal, List<ToolCall> executedToolCalls, Memory memory) {
        try {
            String prompt = String.format("""
                    用户请求: %s

                    已完成以下步骤:
                    %s

                    请用简洁的语言向用户报告执行结果。
                    """,
                    goal,
                    executedToolCalls.stream()
                            .map(tc -> "- " + tc.name() + ": " + tc.parameters())
                            .reduce("", (a, b) -> a + b + "\n")
            );

            LLMResponse response = llmClient.chat(List.of(Message.user(prompt)), List.of());
            return response.content();
        } catch (Exception e) {
            log.warn("生成最终回复失败", e);
            return "任务已完成。";
        }
    }
}
