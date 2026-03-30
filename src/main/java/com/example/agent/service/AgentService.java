package com.example.agent.service;

import com.example.agent.core.Agent;
import com.example.agent.core.AgentRequest;
import com.example.agent.core.AgentResponse;
import com.example.agent.router.IntentRecognitionService;
import com.example.agent.router.IntentType;
import com.example.agent.router.SessionManager;
import com.example.agent.runtime.PlanningAgentRuntime;
import com.example.agent.runtime.SimpleAgentRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final IntentRecognitionService intentRecognitionService;
    private final SimpleAgentRuntime simpleAgentRuntime;
    private final PlanningAgentRuntime planningAgentRuntime;
    private final SessionManager sessionManager;

    public AgentService(
            IntentRecognitionService intentRecognitionService,
            SimpleAgentRuntime simpleAgentRuntime,
            PlanningAgentRuntime planningAgentRuntime,
            SessionManager sessionManager
    ) {
        this.intentRecognitionService = intentRecognitionService;
        this.simpleAgentRuntime = simpleAgentRuntime;
        this.planningAgentRuntime = planningAgentRuntime;
        this.sessionManager = sessionManager;
    }

    public AgentExecutionResult execute(AgentRequest request) {
        // 意图识别
        IntentType intentType = intentRecognitionService.recognize(request);
        log.info("用户输入: {}, 识别意图: {}", truncate(request.userInput(), 50), intentType);

        // 根据意图类型选择运行时
        Agent agent = selectAgent(intentType);
        String agentId = getAgentId(intentType);

        // 执行
        AgentResponse response = agent.execute(request);

        return new AgentExecutionResult(agentId, response);
    }

    private Agent selectAgent(IntentType intentType) {
        return switch (intentType) {
            case PLANNING -> {
                log.info("选择 PlanningAgentRuntime 处理复杂任务");
                yield planningAgentRuntime;
            }
            default -> {
                log.info("选择 SimpleAgentRuntime 处理任务");
                yield simpleAgentRuntime;
            }
        };
    }

    private String getAgentId(IntentType intentType) {
        return switch (intentType) {
            case PLANNING -> "planning-agent";
            case IMAGE_GENERATION -> "image-agent";
            case QUERY -> "query-agent";
            case TOOL_CALL -> "tool-agent";
            case CHAT -> "chat-agent";
            default -> "general-agent";
        };
    }

    public void clearSession(String userId, String sessionId) {
        sessionManager.clear(userId, sessionId);
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
