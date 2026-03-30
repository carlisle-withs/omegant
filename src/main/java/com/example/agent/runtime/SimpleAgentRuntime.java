package com.example.agent.runtime;

import com.example.agent.config.AgentProperties;
import com.example.agent.core.Agent;
import com.example.agent.core.AgentRequest;
import com.example.agent.core.AgentResponse;
import com.example.agent.core.LLMClient;
import com.example.agent.core.LLMResponse;
import com.example.agent.core.Memory;
import com.example.agent.core.Message;
import com.example.agent.core.Tool;
import com.example.agent.core.ToolCall;
import com.example.agent.core.ToolResult;
import com.example.agent.router.SessionManager;
import com.example.agent.tools.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SimpleAgentRuntime implements Agent {

    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final SessionManager sessionManager;
    private final AgentProperties agentProperties;

    public SimpleAgentRuntime(
            LLMClient llmClient,
            ToolRegistry toolRegistry,
            SessionManager sessionManager,
            AgentProperties agentProperties
    ) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.sessionManager = sessionManager;
        this.agentProperties = agentProperties;
    }

    @Override
    public AgentResponse execute(AgentRequest request) {
        Memory memory = sessionManager.getOrCreate(request.userId(), request.sessionId());
        memory.add(Message.user(request.userInput()));

        List<ToolCall> executedToolCalls = new ArrayList<>();

        for (int iteration = 0; iteration < agentProperties.getRuntime().getMaxIterations(); iteration++) {
            LLMResponse llmResponse = llmClient.chat(memory.messages(), toolRegistry.allTools());
            if (llmResponse.toolCalls().isEmpty()) {
                memory.add(Message.assistant(llmResponse.content()));
                return new AgentResponse(llmResponse.content(), "DONE", List.copyOf(executedToolCalls));
            }

            memory.add(Message.assistantToolCalls(llmResponse.content(), llmResponse.toolCalls()));
            for (ToolCall toolCall : llmResponse.toolCalls()) {
                executedToolCalls.add(toolCall);
                ToolResult toolResult = executeTool(toolCall);
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("success", toolResult.success());
                if (toolCall.id() != null) {
                    metadata.put("toolCallId", toolCall.id());
                }
                memory.add(Message.tool(toolCall.name(), formatToolOutput(toolCall.name(), toolResult), metadata));
            }
        }

        String content = "执行达到最大迭代次数，请简化问题或增强工具策略。";
        memory.add(Message.assistant(content));
        return new AgentResponse(content, "MAX_ITERATIONS", List.copyOf(executedToolCalls));
    }

    private ToolResult executeTool(ToolCall toolCall) {
        return toolRegistry.get(toolCall.name())
                .map(tool -> safeExecute(tool, toolCall.parameters()))
                .orElseGet(() -> ToolResult.failure("未找到工具: " + toolCall.name()));
    }

    private ToolResult safeExecute(Tool tool, Map<String, Object> parameters) {
        try {
            return tool.execute(parameters);
        } catch (Exception exception) {
            return ToolResult.failure(exception.getMessage());
        }
    }

    private String formatToolOutput(String toolName, ToolResult result) {
        if (result.success()) {
            return toolName + " => " + result.output();
        }
        return toolName + " => ERROR: " + result.error();
    }
}
