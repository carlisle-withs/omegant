package com.example.agent.llm;

import com.example.agent.core.LLMClient;
import com.example.agent.core.LLMResponse;
import com.example.agent.core.Message;
import com.example.agent.core.Tool;

import java.util.List;

public class FallbackLLMClient implements LLMClient {

    private final LLMClient primary;
    private final LLMClient fallback;

    public FallbackLLMClient(LLMClient primary, LLMClient fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public LLMResponse chat(List<Message> messages, List<Tool> availableTools) {
        try {
            LLMResponse response = primary.chat(messages, availableTools);
            if (response == null) {
                return fallback.chat(messages, availableTools);
            }
            String content = response.content();
            if (content != null && (content.startsWith("大模型调用失败：") || content.startsWith("大模型未返回有效结果"))) {
                return fallback.chat(messages, availableTools);
            }
            return response;
        } catch (Exception exception) {
            return fallback.chat(messages, availableTools);
        }
    }
}
