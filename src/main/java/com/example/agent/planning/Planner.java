package com.example.agent.planning;

import com.example.agent.core.AgentRequest;
import com.example.agent.core.Message;
import com.example.agent.core.Tool;
import com.example.agent.core.ToolResult;

import java.util.List;

/**
 * 计划器接口
 */
public interface Planner {

    /**
     * 为请求创建执行计划
     */
    Plan createPlan(AgentRequest request, List<Message> conversationHistory, List<Tool> availableTools);

    /**
     * 判断是否需要重新规划
     */
    boolean shouldReplan(Plan currentPlan, ToolResult lastResult, List<Message> recentContext);

    /**
     * 重新规划
     */
    default Plan replan(Plan currentPlan, List<Message> recentContext, List<Tool> availableTools) {
        return createPlan(
                new AgentRequest("", "", currentPlan.goal()),
                recentContext,
                availableTools
        );
    }
}
