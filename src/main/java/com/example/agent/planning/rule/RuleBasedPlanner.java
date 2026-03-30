package com.example.agent.planning.rule;

import com.example.agent.core.*;
import com.example.agent.planning.Plan;
import com.example.agent.planning.PlanStep;
import com.example.agent.planning.Planner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于规则的计划器 - 处理简单的模式化请求
 */
@Component
public class RuleBasedPlanner implements Planner {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedPlanner.class);

    private static final Pattern CALC_PATTERN = Pattern.compile("(\\d+\\s*[+\\-*/]\\s*\\d+\\s*)*\\d+");
    private static final Pattern TIME_PATTERN = Pattern.compile(".*(时间|几点|现在几点了|today|time|now).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEATHER_PATTERN = Pattern.compile(".*(天气|气温|温度|weather).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_PATTERN = Pattern.compile(".*(生成|画|照片|image|photo|picture).*", Pattern.CASE_INSENSITIVE);

    @Override
    public Plan createPlan(AgentRequest request, List<Message> conversationHistory, List<Tool> availableTools) {
        String userInput = request.userInput().toLowerCase();
        List<PlanStep> steps = new ArrayList<>();

        // 检测计算请求
        if (containsCalculation(userInput)) {
            String expression = extractCalculation(userInput);
            if (expression != null) {
                steps.add(new PlanStep(
                        1,
                        "执行数学计算",
                        "calculator",
                        Map.of("expression", expression)
                ));
                return new Plan(request.userInput(), steps);
            }
        }

        // 检测时间查询
        if (TIME_PATTERN.matcher(userInput).matches()) {
            String zoneId = extractTimeZone(userInput);
            steps.add(new PlanStep(
                    1,
                    "查询当前时间",
                    "time",
                    zoneId != null ? Map.of("zoneId", zoneId) : Map.of()
            ));
            return new Plan(request.userInput(), steps);
        }

        // 检测天气查询
        if (WEATHER_PATTERN.matcher(userInput).matches()) {
            String city = extractCity(userInput);
            if (city != null) {
                steps.add(new PlanStep(
                        1,
                        "查询天气",
                        "weather",
                        Map.of("city", city)
                ));
                return new Plan(request.userInput(), steps);
            }
        }

        // 检测图片生成
        if (IMAGE_PATTERN.matcher(userInput).matches()) {
            steps.add(new PlanStep(
                    1,
                    "生成图片",
                    "mcp_dashscope-zimage_modelstudio_z_image_generation",
                    Map.of("prompt", request.userInput(), "size", "1024*1024")
            ));
            return new Plan(request.userInput(), steps);
        }

        // 无法规划
        return new Plan(request.userInput(), steps);
    }

    @Override
    public boolean shouldReplan(Plan currentPlan, ToolResult lastResult, List<Message> recentContext) {
        return !lastResult.success();
    }

    private boolean containsCalculation(String input) {
        return input.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*");
    }

    private String extractCalculation(String input) {
        Matcher matcher = CALC_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    private String extractTimeZone(String input) {
        if (input.contains("北京") || input.contains("中国")) {
            return "Asia/Shanghai";
        }
        if (input.contains("东京") || input.contains("日本")) {
            return "Asia/Tokyo";
        }
        if (input.contains("纽约") || input.contains("美国")) {
            return "America/New_York";
        }
        return null;
    }

    private String extractCity(String input) {
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆"};
        for (String city : cities) {
            if (input.contains(city)) {
                return city;
            }
        }
        return "北京"; // 默认
    }
}
