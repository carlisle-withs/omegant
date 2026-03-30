package com.example.agent.core;

public record AgentRequest(
        String userId,
        String sessionId,
        String userInput
) {
}
