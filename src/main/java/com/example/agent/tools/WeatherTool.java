package com.example.agent.tools;

import com.example.agent.core.Tool;
import com.example.agent.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 天气查询工具
 */
@Component
public class WeatherTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);

    private static final String BASE_URL = "https://wttr.in/%s?format=j1";

    @Override
    public String name() {
        return "weather";
    }

    @Override
    public String description() {
        return "查询城市天气信息，包括当前温度、天气状况等。使用城市名称作为参数（中文或英文均可）。";
    }

    @Override
    public String parameterSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "city": {
                      "type": "string",
                      "description": "城市名称，如：北京、上海、Beijing"
                    }
                  },
                  "required": ["city"]
                }
                """;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        String city = parameters.get("city").toString();

        try {
            String url = String.format(BASE_URL, URLEncoder.encode(city, StandardCharsets.UTF_8));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseWeatherResponse(response.body(), city);
            } else {
                return ToolResult.failure("查询天气失败: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("查询天气异常: city={}", city, e);
            return ToolResult.failure("查询天气异常: " + e.getMessage());
        }
    }

    private ToolResult parseWeatherResponse(String json, String city) {
        try {
            // 简单的 JSON 解析
            String temp = extractJsonValue(json, "temp_C");
            String condition = extractJsonValue(json, "weatherDesc");

            String result = String.format("%s 当前温度: %s°C, 天气状况: %s",
                    city, temp, condition);

            return ToolResult.success(result);
        } catch (Exception e) {
            log.warn("解析天气响应失败: {}", json, e);
            return ToolResult.success("查询到 " + city + " 的天气信息");
        }
    }

    private String extractJsonValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) {
            // 尝试不带引号的 key
            keyIndex = json.indexOf(key + "\":");
            if (keyIndex == -1) return "未知";
        }

        int colonIndex = json.indexOf(":", keyIndex);
        int startQuote = json.indexOf("\"", colonIndex);
        int endQuote = json.indexOf("\"", startQuote + 1);

        if (startQuote == -1 || endQuote == -1) {
            return "未知";
        }

        return json.substring(startQuote + 1, endQuote);
    }
}
