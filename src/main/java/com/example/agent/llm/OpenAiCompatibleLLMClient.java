package com.example.agent.llm;

import com.example.agent.config.AgentProperties;
import com.example.agent.core.ChatChunk;
import com.example.agent.core.LLMClient;
import com.example.agent.core.LLMResponse;
import com.example.agent.core.Message;
import com.example.agent.core.MessageRole;
import com.example.agent.core.Tool;
import com.example.agent.core.ToolCall;
import com.example.agent.rag.ConversationRagService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleLLMClient implements LLMClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AgentProperties agentProperties;
    private final ConversationRagService conversationRagService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiCompatibleLLMClient(
            AgentProperties agentProperties,
            ConversationRagService conversationRagService,
            ObjectMapper objectMapper
    ) {
        this.agentProperties = agentProperties;
        this.conversationRagService = conversationRagService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(agentProperties.getLlm().getBaseUrl()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + agentProperties.getLlm().getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public LLMResponse chat(List<Message> messages, List<Tool> availableTools) {
        try {
            JsonNode response = restClient.post()
                    .uri("/v1/chat/completions")
                    .body(buildRequest(messages, availableTools))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode messageNode = response == null
                    ? null
                    : response.path("choices").isEmpty() ? null : response.path("choices").get(0).path("message");
            if (messageNode == null || messageNode.isMissingNode()) {
                return new LLMResponse("大模型未返回有效结果。", List.of());
            }

            return new LLMResponse(extractContent(messageNode.path("content")), extractToolCalls(messageNode.path("tool_calls")));
        } catch (Exception exception) {
            return new LLMResponse("大模型调用失败：" + exception.getMessage(), List.of());
        }
    }

    @Override
    public Flux<ChatChunk> streamChat(List<Message> messages, List<Tool> availableTools) {
        // Fallback: convert sync response to streaming
        LLMResponse response = chat(messages, availableTools);
        if (!response.toolCalls().isEmpty()) {
            return Flux.just(ChatChunk.done("stop"));
        }
        // Stream content character by character
        String content = response.content();
        if (content == null || content.isEmpty()) {
            return Flux.just(ChatChunk.done("stop"));
        }
        return Flux.range(0, content.length())
                .map(i -> ChatChunk.content(String.valueOf(content.charAt(i))))
                .concatWith(Flux.just(ChatChunk.done("stop")));
    }

    private Map<String, Object> buildRequest(List<Message> messages, List<Tool> availableTools) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", agentProperties.getLlm().getChatModel());
        request.put("temperature", agentProperties.getLlm().getTemperature());
        request.put("messages", buildMessages(messages));
        if (!availableTools.isEmpty()) {
            request.put("tools", buildTools(availableTools));
            request.put("tool_choice", "auto");
        }
        return request;
    }

    private List<Map<String, Object>> buildMessages(List<Message> messages) {
        List<Map<String, Object>> requestMessages = new ArrayList<>();

        String ragContext = conversationRagService.buildContext(messages);
        requestMessages.add(systemMessage(ragContext));

        int fromIndex = Math.max(0, messages.size() - agentProperties.getLlm().getMaxContextMessages());
        for (Message message : messages.subList(fromIndex, messages.size())) {
            requestMessages.add(toApiMessage(message));
        }
        return requestMessages;
    }

    private Map<String, Object> systemMessage(String ragContext) {
        StringBuilder content = new StringBuilder("""
                你是一个企业级 Agent 运行时助手。
                你的目标是准确理解用户意图，必要时调用工具，并基于工具结果或检索上下文给出最终答案。
                当问题涉及计算、时间或外部知识时，请优先考虑工具调用。
                """.strip());
        if (ragContext != null && !ragContext.isBlank()) {
            content.append(System.lineSeparator()).append(System.lineSeparator()).append(ragContext);
        }

        return Map.of(
                "role", "system",
                "content", content.toString()
        );
    }

    private Map<String, Object> toApiMessage(Message message) {
        if (message.role() == MessageRole.TOOL) {
            Map<String, Object> toolMessage = new HashMap<>();
            toolMessage.put("role", "tool");
            toolMessage.put("content", message.content());
            Object toolCallId = message.metadata().get("toolCallId");
            if (toolCallId != null) {
                toolMessage.put("tool_call_id", toolCallId);
            }
            toolMessage.put("name", message.name());
            return toolMessage;
        }

        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("role", message.role() == MessageRole.USER ? "user" : "assistant");
        mapped.put("content", message.content() == null || message.content().isBlank() ? "" : message.content());

        Object toolCallsMetadata = message.metadata().get("toolCalls");
        if (toolCallsMetadata instanceof List<?> toolCalls && !toolCalls.isEmpty()) {
            mapped.put("tool_calls", toolCalls.stream()
                    .filter(ToolCall.class::isInstance)
                    .map(ToolCall.class::cast)
                    .map(this::toApiToolCall)
                    .toList());
        }
        return mapped;
    }

    private Map<String, Object> toApiToolCall(ToolCall toolCall) {
        return Map.of(
                "id", toolCall.id() == null ? toolCall.name() + "-call" : toolCall.id(),
                "type", "function",
                "function", Map.of(
                        "name", toolCall.name(),
                        "arguments", toJson(toolCall.parameters())
                )
        );
    }

    private List<Map<String, Object>> buildTools(List<Tool> availableTools) {
        return availableTools.stream()
                .map(tool -> Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", tool.name(),
                                "description", tool.description(),
                                "parameters", parseSchema(tool.parameterSchema())
                        )
                ))
                .toList();
    }

    private JsonNode parseSchema(String schema) {
        try {
            return objectMapper.readTree(schema);
        } catch (Exception exception) {
            throw new IllegalArgumentException("工具 Schema 非法: " + schema, exception);
        }
    }

    private List<ToolCall> extractToolCalls(JsonNode toolCallsNode) {
        if (toolCallsNode == null || toolCallsNode.isMissingNode() || !toolCallsNode.isArray()) {
            return List.of();
        }

        List<ToolCall> toolCalls = new ArrayList<>();
        for (JsonNode toolCallNode : toolCallsNode) {
            JsonNode functionNode = toolCallNode.path("function");
            toolCalls.add(new ToolCall(
                    toolCallNode.path("id").asText(null),
                    functionNode.path("name").asText(),
                    parseArguments(functionNode.path("arguments").asText("{}"))
            ));
        }
        return toolCalls;
    }

    private Map<String, Object> parseArguments(String arguments) {
        try {
            return objectMapper.readValue(arguments, MAP_TYPE);
        } catch (Exception exception) {
            return Map.of("raw", arguments);
        }
    }

    private String extractContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode part : contentNode) {
                if (part.path("type").asText().equals("text")) {
                    if (!builder.isEmpty()) {
                        builder.append(System.lineSeparator());
                    }
                    builder.append(part.path("text").asText());
                }
            }
            return builder.toString();
        }
        return contentNode.toString();
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("工具参数序列化失败", exception);
        }
    }

    private String trimTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
