package com.example.agent.tools;

import com.example.agent.core.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CalculatorToolTest {

    private final CalculatorTool calculatorTool = new CalculatorTool();

    @Test
    void shouldEvaluateExpression() {
        ToolResult result = calculatorTool.execute(Map.of("expression", "(2 + 3) * 4"));

        assertThat(result.success()).isTrue();
        assertThat(result.output()).isEqualTo("20");
    }

    @Test
    void shouldRejectDivisionByZero() {
        ToolResult result = calculatorTool.execute(Map.of("expression", "8 / 0"));

        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("除数不能为 0");
    }
}
