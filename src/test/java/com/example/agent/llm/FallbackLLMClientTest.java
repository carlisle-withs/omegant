package com.example.agent.llm;

import com.example.agent.core.LLMClient;
import com.example.agent.core.LLMResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackLLMClientTest {

    @Test
    void shouldFallbackWhenPrimaryReturnsFailureContent() {
        LLMClient primary = (messages, tools) -> new LLMResponse("大模型调用失败：network", List.of());
        LLMClient fallback = (messages, tools) -> new LLMResponse("fallback", List.of());

        FallbackLLMClient client = new FallbackLLMClient(primary, fallback);

        LLMResponse response = client.chat(List.of(), List.of());

        assertThat(response.content()).isEqualTo("fallback");
    }
}
