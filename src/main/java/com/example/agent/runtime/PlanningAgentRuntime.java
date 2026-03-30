package com.example.agent.runtime;

import com.example.agent.config.AgentProperties;
import com.example.agent.core.*;
import com.example.agent.planning.*;
import com.example.agent.router.SessionManager;
import com.example.agent.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于规划的执行器
 * 支持复杂任务的多步骤规划和执行
 */
@Component
public class PlanningAgentRuntime implements Agent {

    private static final Logger log = LoggerFactory.getLogger(PlanningAgentRuntime.class);

    private final Planner planner;
    private final PlanExecutor planExecutor;
    private final SessionManager sessionManager;
    private final ToolRegistry toolRegistry;
    private final AgentProperties agentProperties;

    public PlanningAgentRuntime(
            Planner planner,
            PlanExecutor planExecutor,
            SessionManager sessionManager,
            ToolRegistry toolRegistry,
            AgentProperties agentProperties
    ) {
        this.planner = planner;
        this.planExecutor = planExecutor;
        this.sessionManager = sessionManager;
        this.toolRegistry = toolRegistry;
        this.agentProperties = agentProperties;
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        log.info("PlanningAgentRuntime executing request: {}", request.userInput());

        Memory memory = sessionManager.getOrCreate(request.userId(), request.sessionId());
        memory.add(Message.user(request.userInput()));

        // 创建计划
        Plan plan = planner.createPlan(request, memory.messages(), toolRegistry.allTools());

        log.info("创建计划，共 {} 个步骤: {}", plan.totalSteps(), plan.goal());

        if (plan.steps().isEmpty()) {
            String response = "无法为您的请求创建执行计划。";
            memory.add(Message.assistant(response));
            return new AgentResponse(response, PlanStatus.FAILURE.name(), List.of());
        }

        // 执行计划
        var executionResult = planExecutor.executePlan(plan, memory);

        String finalResponse = buildFinalResponse(executionResult);
        memory.add(Message.assistant(finalResponse));

        log.info("计划执行完成，状态: {}", executionResult.status());

        return new AgentResponse(
                finalResponse,
                executionResult.status().name(),
                executionResult.executedToolCalls()
        );
    }

    private String buildFinalResponse(PlanExecutionResult result) {
        if (result.isSuccess()) {
            return result.finalResponse();
        } else if (result.isFailure()) {
            return "执行失败: " + result.finalResponse();
        } else {
            return "执行中: " + result.finalResponse();
        }
    }
}
