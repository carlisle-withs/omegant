package com.example.agent.core;

import java.util.List;

import reactor.core.publisher.Flux;

public interface LLMClient {
    LLMResponse chat(List<Message> messages, List<Tool> availableTools);

    default Flux<ChatChunk> streamChat(List<Message> messages, List<Tool> availableTools) {
        return Flux.empty();
    }
}
