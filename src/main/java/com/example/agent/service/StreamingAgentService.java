package com.example.agent.service;

import com.example.agent.core.*;
import com.example.agent.router.SessionManager;
import com.example.agent.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流式 Agent 服务
 * 支持 SSE 流式输出，实时推送 LLM 响应和工具调用状态
 */
@Service
public class StreamingAgentService {

    private static final Logger log = LoggerFactory.getLogger(StreamingAgentService.class);

    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final SessionManager sessionManager;

    public StreamingAgentService(
            LLMClient llmClient,
            ToolRegistry toolRegistry,
            SessionManager sessionManager
    ) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.sessionManager = sessionManager;
    }

    /**
     * 创建 SSE 流式响应
     */
    public Flux<ServerSentEvent<String>> streamChat(AgentRequest request) {
        Memory memory = sessionManager.getOrCreate(request.userId(), request.sessionId());
        memory.add(Message.user(request.userInput()));

        List<ToolCall> executedToolCalls = new ArrayList<>();
        StringBuilder accumulatedContent = new StringBuilder();

        return llmClient.streamChat(memory.messages(), toolRegistry.allTools())
                .flatMap(chunk -> {
                    if (chunk.toolCall() != null) {
                        ToolCall toolCall = chunk.toolCall();
                        executedToolCalls.add(toolCall);
                        String eventData = String.format(
                                "{\"type\":\"tool_call\",\"tool\":\"%s\",\"params\":%s}",
                                toolCall.name(),
                                toJson(toolCall.parameters())
                        );
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("tool_call")
                                .data(eventData)
                                .build());
                    }

                    if (chunk.delta() != null) {
                        accumulatedContent.append(chunk.delta());
                        String eventData = String.format(
                                "{\"type\":\"content\",\"delta\":\"%s\"}",
                                escapeJson(chunk.delta())
                        );
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("content")
                                .data(eventData)
                                .build());
                    }

                    if (chunk.complete() != null && chunk.complete()) {
                        finishReasons.add(chunk.finishReason() != null ? chunk.finishReason() : "DONE");
                        memory.add(Message.assistant(accumulatedContent.toString()));
                        return executeToolCalls(memory, executedToolCalls, accumulatedContent.toString());
                    }

                    return Flux.empty();
                })
                .startWith(ServerSentEvent.<String>builder()
                        .event("start")
                        .data("{\"type\":\"start\"}")
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .event("done")
                        .data(String.format("{\"type\":\"done\",\"content\":\"%s\"}",
                                escapeJson(accumulatedContent.toString())))
                        .build()));
    }

    private List<String> finishReasons = new ArrayList<>();

    private Flux<ServerSentEvent<String>> executeToolCalls(
            Memory memory,
            List<ToolCall> toolCalls,
            String assistantContent
    ) {
        if (toolCalls.isEmpty()) {
            return Flux.empty();
        }

        Flux<ServerSentEvent<String>> result = Flux.empty();

        for (ToolCall toolCall : toolCalls) {
            result = result.concatWith(Flux.defer(() -> {
                memory.add(Message.assistantToolCalls(assistantContent, List.of(toolCall)));

                ToolResult toolResult = executeTool(toolCall);
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("success", toolResult.success());
                if (toolCall.id() != null) {
                    metadata.put("toolCallId", toolCall.id());
                }
                String toolOutput = formatToolOutput(toolCall.name(), toolResult);
                memory.add(Message.tool(toolCall.name(), toolOutput, metadata));

                String eventData = String.format(
                        "{\"type\":\"tool_result\",\"tool\":\"%s\",\"result\":\"%s\",\"success\":%s}",
                        toolCall.name(),
                        escapeJson(toolResult.success() ? toolResult.output() : toolResult.error()),
                        toolResult.success()
                );
                return Flux.just(ServerSentEvent.<String>builder()
                        .event("tool_result")
                        .data(eventData)
                        .build());
            }));
        }

        return result;
    }

    private ToolResult executeTool(ToolCall toolCall) {
        return toolRegistry.get(toolCall.name())
                .map(tool -> {
                    try {
                        return tool.execute(toolCall.parameters());
                    } catch (Exception e) {
                        return ToolResult.failure(e.getMessage());
                    }
                })
                .orElseGet(() -> ToolResult.failure("未找到工具: " + toolCall.name()));
    }

    private String formatToolOutput(String toolName, ToolResult result) {
        if (result.success()) {
            return toolName + " => " + result.output();
        }
        return toolName + " => ERROR: " + result.error();
    }

    private String toJson(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else {
                sb.append("\"").append(escapeJson(value.toString())).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
