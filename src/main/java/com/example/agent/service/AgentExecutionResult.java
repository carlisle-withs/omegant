package com.example.agent.service;

import com.example.agent.core.AgentResponse;

public record AgentExecutionResult(
        String agentId,
        AgentResponse response
) {
}
