package com.example.agent.router;

import com.example.agent.core.AgentRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AgentRouter {

    public String route(AgentRequest request) {
        String input = request.userInput().toLowerCase(Locale.ROOT);
        if (input.contains("架构") || input.contains("memory") || input.contains("session")
                || input.contains("rag") || input.contains("tool") || input.contains("agent")) {
            return "architecture-agent";
        }
        if (input.contains("时间") || input.contains("几点")) {
            return "time-agent";
        }
        if (containsCalculation(input)) {
            return "calculator-agent";
        }
        return "general-agent";
    }

    private boolean containsCalculation(String input) {
        return input.contains("计算")
                || input.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*");
    }
}
