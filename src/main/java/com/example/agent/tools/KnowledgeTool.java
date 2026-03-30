package com.example.agent.tools;

import com.example.agent.core.Tool;
import com.example.agent.core.ToolResult;
import com.example.agent.rag.ConversationRagService;

import java.util.Map;

public class KnowledgeTool implements Tool {

    private final ConversationRagService conversationRagService;

    public KnowledgeTool(ConversationRagService conversationRagService) {
        this.conversationRagService = conversationRagService;
    }

    @Override
    public String name() {
        return "knowledge";
    }

    @Override
    public String description() {
        return "检索内置企业 Agent 知识库，回答架构、Memory、Session、RAG、治理等问题。";
    }

    @Override
    public String parameterSchema() {
        return """
                {"type":"object","properties":{"query":{"type":"string"}},"required":["query"]}
                """;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        Object queryValue = parameters.get("query");
        if (!(queryValue instanceof String query) || query.isBlank()) {
            return ToolResult.failure("query 参数不能为空");
        }
        String result = conversationRagService.search(query);
        if (result.isBlank()) {
            return ToolResult.failure("知识库中没有匹配结果");
        }
        return ToolResult.success(result);
    }
}
