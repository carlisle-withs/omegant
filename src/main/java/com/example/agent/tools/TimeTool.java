package com.example.agent.tools;

import com.example.agent.core.Tool;
import com.example.agent.core.ToolResult;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class TimeTool implements Tool {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    @Override
    public String name() {
        return "time";
    }

    @Override
    public String description() {
        return "查询指定时区的当前时间。";
    }

    @Override
    public String parameterSchema() {
        return """
                {"type":"object","properties":{"zoneId":{"type":"string"}}}
                """;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String zoneId = String.valueOf(parameters.getOrDefault("zoneId", "Asia/Shanghai"));
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(zoneId));
            return ToolResult.success(now.format(FORMATTER));
        } catch (Exception exception) {
            return ToolResult.failure("无效时区: " + zoneId);
        }
    }
}
