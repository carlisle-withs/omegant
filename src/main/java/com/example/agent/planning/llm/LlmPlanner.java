package com.example.agent.planning.llm;

import com.example.agent.core.*;
import com.example.agent.planning.Plan;
import com.example.agent.planning.PlanStep;
import com.example.agent.planning.Planner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 LLM 的计划器
 */
@Component
@org.springframework.context.annotation.Primary
public class LlmPlanner implements Planner {

    private static final Logger log = LoggerFactory.getLogger(LlmPlanner.class);

    private final LLMClient llmClient;
    private final ObjectMapper objectMapper;

    public LlmPlanner(LLMClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Plan createPlan(AgentRequest request, List<Message> conversationHistory, List<Tool> availableTools) {
        String userInput = request.userInput();

        try {
            String prompt = buildPlanningPrompt(userInput, availableTools);

            List<Message> messages = List.of(
                    Message.user(prompt)
            );

            LLMResponse response = llmClient.chat(messages, List.of());

            return parsePlanResponse(userInput, response.content());
        } catch (Exception e) {
            log.error("创建计划失败", e);
            return new Plan(userInput, List.of());
        }
    }

    @Override
    public boolean shouldReplan(Plan currentPlan, ToolResult lastResult, List<Message> recentContext) {
        if (!lastResult.success()) {
            return true;
        }
        return false;
    }

    private String buildPlanningPrompt(String userInput, List<Tool> availableTools) {
        StringBuilder toolDescriptions = new StringBuilder();
        for (Tool tool : availableTools) {
            toolDescriptions.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");
        }

        return String.format("""
                用户请求: %s

                可用工具:
                %s

                请将用户请求分解为具体的执行步骤，每个步骤应该：
                1. 有一个清晰的描述
                2. 指定要使用的工具
                3. 提供工具所需的参数

                请以 JSON 格式输出步骤列表，格式如下：
                {
                  "steps": [
                    {"index": 1, "description": "步骤描述", "tool": "工具名称", "parameters": {"param1": "value1"}}
                  ]
                }

                如果用户请求很简单，不需要多个步骤，可以返回一个空的 steps 数组。
                """, userInput, toolDescriptions);
    }

    private Plan parsePlanResponse(String goal, String content) {
        try {
            int start = content.indexOf("{");
            int end = content.lastIndexOf("}");
            if (start == -1 || end == -1) {
                return new Plan(goal, List.of());
            }

            String jsonStr = content.substring(start, end + 1);
            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode stepsNode = root.path("steps");

            if (!stepsNode.isArray()) {
                return new Plan(goal, List.of());
            }

            List<PlanStep> steps = new ArrayList<>();
            for (JsonNode stepNode : stepsNode) {
                int index = stepNode.path("index").asInt(steps.size() + 1);
                String description = stepNode.path("description").asText();
                String tool = stepNode.path("tool").asText();
                Map<String, Object> params = objectMapper.convertValue(stepNode.path("parameters"), Map.class);

                steps.add(new PlanStep(index, description, tool, params));
            }

            return new Plan(goal, steps);
        } catch (Exception e) {
            log.warn("解析计划响应失败: {}", content, e);
            return new Plan(goal, List.of());
        }
    }
}
