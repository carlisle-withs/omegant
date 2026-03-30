package com.example.agent.router;

import com.example.agent.core.AgentRequest;
import com.example.agent.core.LLMClient;
import com.example.agent.core.LLMResponse;
import com.example.agent.core.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 LLM 的意图识别服务
 */
@Component
public class LlmIntentRecognitionService implements IntentRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(LlmIntentRecognitionService.class);

    private final LLMClient llmClient;
    private final ObjectMapper objectMapper;

    public LlmIntentRecognitionService(LLMClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public IntentType recognize(AgentRequest request) {
        return recognizeWithConfidence(request).type();
    }

    @Override
    public IntentRecognitionResult recognizeWithConfidence(AgentRequest request) {
        String userInput = request.userInput().toLowerCase();

        // 首先使用规则快速判断
        IntentRecognitionResult ruleResult = quickRuleMatch(userInput);
        if (ruleResult != null && ruleResult.confidence() >= 0.9) {
            log.debug("意图通过规则匹配: {}", ruleResult.type());
            return ruleResult;
        }

        // 对于复杂任务，使用 LLM 判断
        if (isComplexTask(userInput)) {
            return recognizeWithLlm(request);
        }

        // 简单对话
        if (isSimpleChat(userInput)) {
            return IntentRecognitionResult.of(IntentType.CHAT, 0.9, "简单对话");
        }

        // 其他默认归类为工具调用
        return IntentRecognitionResult.of(IntentType.TOOL_CALL, 0.7, "可能需要工具调用");
    }

    private IntentRecognitionResult quickRuleMatch(String input) {
        // 图片生成关键词
        if (containsAny(input, "生成图片", "生成图像", "画", "照片", "图像生成", "生成一张", "image")) {
            return IntentRecognitionResult.of(IntentType.IMAGE_GENERATION, 0.95, "包含图片生成关键词");
        }

        // 规划关键词
        if (containsAny(input, "计划", "规划", "步骤", "如何实现", "怎么做", "流程", "步骤", "plan")) {
            return IntentRecognitionResult.of(IntentType.PLANNING, 0.95, "包含规划关键词");
        }

        // 搜索查询关键词
        if (containsAny(input, "查找", "搜索", "查询", "找", "知道", "是什么", "什么是", "搜索", "search")) {
            return IntentRecognitionResult.of(IntentType.QUERY, 0.9, "包含查询关键词");
        }

        // 天气查询
        if (containsAny(input, "天气", "气温", "温度", "下雨", "weather")) {
            return IntentRecognitionResult.of(IntentType.TOOL_CALL, 0.95, "天气查询");
        }

        // 计算
        if (containsAny(input, "计算", "等于", "+", "-", "*", "/") && containsDigit(input)) {
            return IntentRecognitionResult.of(IntentType.TOOL_CALL, 0.95, "数学计算");
        }

        // 时间查询
        if (containsAny(input, "时间", "几点", "什么时候", "现在", "today", "now", "time")) {
            return IntentRecognitionResult.of(IntentType.TOOL_CALL, 0.95, "时间查询");
        }

        return null;
    }

    private boolean isComplexTask(String input) {
        // 需要多步骤的任务特征
        return containsAny(input, "首先", "然后", "最后", "接下来", "步骤", "流程",
                "帮我", "请帮我", "能不能", "是否可以", "可以吗", "实现", "完成",
                "多个", "几次", "几个", "分别", "不同的");
    }

    private boolean isSimpleChat(String input) {
        // 简单问候和闲聊
        return containsAny(input, "你好", "嗨", "hi", "hello", "您好", "在吗", "在不在",
                "谢谢", "感谢", "再见", "拜拜", "帮忙", "问一下", "问问");
    }

    private IntentRecognitionResult recognizeWithLlm(AgentRequest request) {
        try {
            String prompt = buildIntentPrompt(request.userInput());

            List<Message> messages = List.of(
                    Message.user(prompt)
            );

            LLMResponse response = llmClient.chat(messages, List.of());

            return parseLlmResponse(response.content());
        } catch (Exception e) {
            log.warn("LLM 意图识别失败，使用默认分类: {}", e.getMessage());
            return IntentRecognitionResult.of(IntentType.TOOL_CALL, 0.5, "LLM识别失败");
        }
    }

    private String buildIntentPrompt(String userInput) {
        return String.format("""
                分析以下用户输入，判断用户意图类型。

                用户输入: %s

                意图类型说明:
                - CHAT: 简单对话、寒暄、问候
                - TOOL_CALL: 需要调用单个工具（天气、计算、时间等）
                - PLANNING: 需要多步骤规划和执行的复杂任务
                - QUERY: 信息查询、搜索
                - IMAGE_GENERATION: 图片生成

                请只输出一个意图类型（如：CHAT 或 TOOL_CALL），不要其他内容。
                """, userInput);
    }

    private IntentRecognitionResult parseLlmResponse(String content) {
        String trimmed = content.trim().toUpperCase();

        try {
            IntentType type = IntentType.valueOf(trimmed);
            return IntentRecognitionResult.of(type, 0.8, "LLM识别");
        } catch (IllegalArgumentException e) {
            log.warn("无法解析 LLM 意图响应: {}", content);
            return IntentRecognitionResult.of(IntentType.UNKNOWN, 0.3, "无法识别");
        }
    }

    private boolean containsAny(String input, String... keywords) {
        for (String keyword : keywords) {
            if (input.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDigit(String input) {
        return input.matches(".*\\d+.*");
    }
}
