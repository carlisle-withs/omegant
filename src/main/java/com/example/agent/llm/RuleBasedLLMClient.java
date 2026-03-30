package com.example.agent.llm;

import com.example.agent.core.LLMClient;
import com.example.agent.core.LLMResponse;
import com.example.agent.core.Message;
import com.example.agent.core.MessageRole;
import com.example.agent.core.Tool;
import com.example.agent.core.ToolCall;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleBasedLLMClient implements LLMClient {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("[0-9().+\\-*/\\s]+");

    @Override
    public LLMResponse chat(List<Message> messages, List<Tool> availableTools) {
        Message lastUserMessage = findLastMessage(messages, MessageRole.USER);
        if (lastUserMessage == null) {
            return new LLMResponse("请先告诉我你的目标。", List.of());
        }

        List<Message> toolMessages = toolMessagesAfterLastUser(messages);
        if (!toolMessages.isEmpty()) {
            return new LLMResponse(summarizeToolMessages(lastUserMessage.content(), toolMessages), List.of());
        }

        Set<String> availableToolNames = availableTools.stream().map(Tool::name).collect(java.util.stream.Collectors.toSet());
        String input = lastUserMessage.content();
        String normalized = input.toLowerCase(Locale.ROOT);

        if (shouldUseCalculator(normalized) && availableToolNames.contains("calculator")) {
            String expression = extractExpression(input);
            if (expression != null) {
                return new LLMResponse("", List.of(new ToolCall("calculator", Map.of("expression", expression))));
            }
        }

        if (shouldUseTime(normalized) && availableToolNames.contains("time")) {
            return new LLMResponse("", List.of(new ToolCall("time", Map.of("zoneId", inferZoneId(normalized)))));
        }

        if (shouldUseKnowledge(normalized) && availableToolNames.contains("knowledge")) {
            return new LLMResponse("", List.of(new ToolCall("knowledge", Map.of("query", input))));
        }

        return new LLMResponse("""
                我已经理解你的意图。
                当前运行时更擅长三类任务：企业 Agent 架构问答、时间查询、数学计算。
                如果你希望扩展为真实生产 Agent，可以继续接入外部 LLM、RAG 向量库、HTTP/API 工具和持久化 Memory。
                """.strip(), List.of());
    }

    private Message findLastMessage(List<Message> messages, MessageRole role) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            Message message = messages.get(index);
            if (message.role() == role) {
                return message;
            }
        }
        return null;
    }

    private List<Message> toolMessagesAfterLastUser(List<Message> messages) {
        int lastUserIndex = -1;
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (messages.get(index).role() == MessageRole.USER) {
                lastUserIndex = index;
                break;
            }
        }
        if (lastUserIndex < 0) {
            return List.of();
        }
        return messages.subList(lastUserIndex + 1, messages.size()).stream()
                .filter(message -> message.role() == MessageRole.TOOL)
                .toList();
    }

    private String summarizeToolMessages(String userInput, List<Message> toolMessages) {
        String toolSummary = toolMessages.stream()
                .map(Message::content)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("");

        if (userInput.toLowerCase(Locale.ROOT).contains("时间") || userInput.contains("几点")) {
            return "已完成时间查询：" + System.lineSeparator() + toolSummary;
        }
        if (shouldUseCalculator(userInput.toLowerCase(Locale.ROOT))) {
            return "已完成计算：" + System.lineSeparator() + toolSummary;
        }
        return "我结合工具结果给出回答：" + System.lineSeparator() + toolSummary;
    }

    private boolean shouldUseCalculator(String input) {
        return input.contains("计算") || EXPRESSION_PATTERN.matcher(input).find();
    }

    private boolean shouldUseTime(String input) {
        return input.contains("时间") || input.contains("几点") || input.contains("time");
    }

    private boolean shouldUseKnowledge(String input) {
        return input.contains("agent")
                || input.contains("架构")
                || input.contains("memory")
                || input.contains("session")
                || input.contains("rag")
                || input.contains("tool")
                || input.contains("治理")
                || input.contains("监控")
                || input.contains("反思")
                || input.contains("规划");
    }

    private String extractExpression(String input) {
        Matcher matcher = EXPRESSION_PATTERN.matcher(input.replace("×", "*").replace("÷", "/"));
        String bestCandidate = null;
        if (matcher.find()) {
            do {
                String candidate = matcher.group().trim();
                if (candidate.matches(".*\\d.*") && candidate.matches(".*[+\\-*/].*")) {
                    if (bestCandidate == null || candidate.length() > bestCandidate.length()) {
                        bestCandidate = candidate;
                    }
                }
            } while (matcher.find());
        }
        return bestCandidate;
    }

    private String inferZoneId(String input) {
        if (input.contains("utc")) {
            return "UTC";
        }
        if (input.contains("东京") || input.contains("tokyo")) {
            return "Asia/Tokyo";
        }
        if (input.contains("纽约") || input.contains("new york")) {
            return "America/New_York";
        }
        return "Asia/Shanghai";
    }
}
