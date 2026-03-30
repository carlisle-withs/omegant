package com.example.agent.tools;

import com.example.agent.core.Tool;
import com.example.agent.core.ToolResult;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public class CalculatorTool implements Tool {

    private static final MathContext MATH_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);

    @Override
    public String name() {
        return "calculator";
    }

    @Override
    public String description() {
        return "执行四则运算表达式，支持括号、加减乘除。";
    }

    @Override
    public String parameterSchema() {
        return """
                {"type":"object","properties":{"expression":{"type":"string"}},"required":["expression"]}
                """;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        Object expressionValue = parameters.get("expression");
        if (!(expressionValue instanceof String expression) || expression.isBlank()) {
            return ToolResult.failure("expression 参数不能为空");
        }

        try {
            BigDecimal result = evaluate(expression);
            return ToolResult.success(result.stripTrailingZeros().toPlainString());
        } catch (RuntimeException exception) {
            return ToolResult.failure(exception.getMessage());
        }
    }

    private BigDecimal evaluate(String expression) {
        List<String> tokens = tokenize(expression.replace("×", "*").replace("÷", "/"));
        List<String> postfix = toPostfix(tokens);
        return evalPostfix(postfix);
    }

    private List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder number = new StringBuilder();

        for (int index = 0; index < expression.length(); index++) {
            char current = expression.charAt(index);

            if (Character.isWhitespace(current)) {
                continue;
            }

            if (Character.isDigit(current) || current == '.') {
                number.append(current);
                continue;
            }

            if (number.length() > 0) {
                tokens.add(number.toString());
                number.setLength(0);
            }

            if (current == '-' && isUnaryMinus(tokens)) {
                number.append(current);
                continue;
            }

            if ("+-*/()".indexOf(current) >= 0) {
                tokens.add(String.valueOf(current));
                continue;
            }

            throw new IllegalArgumentException("表达式包含不支持的字符: " + current);
        }

        if (number.length() > 0) {
            tokens.add(number.toString());
        }

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("表达式不能为空");
        }

        return tokens;
    }

    private boolean isUnaryMinus(List<String> tokens) {
        if (tokens.isEmpty()) {
            return true;
        }
        String previous = tokens.getLast();
        return "(".equals(previous) || isOperator(previous);
    }

    private List<String> toPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Deque<String> operators = new ArrayDeque<>();

        for (String token : tokens) {
            if (isNumber(token)) {
                output.add(token);
                continue;
            }

            if ("(".equals(token)) {
                operators.push(token);
                continue;
            }

            if (")".equals(token)) {
                while (!operators.isEmpty() && !"(".equals(operators.peek())) {
                    output.add(operators.pop());
                }
                if (operators.isEmpty() || !"(".equals(operators.pop())) {
                    throw new IllegalArgumentException("括号不匹配");
                }
                continue;
            }

            while (!operators.isEmpty() && isOperator(operators.peek())
                    && precedence(operators.peek()) >= precedence(token)) {
                output.add(operators.pop());
            }
            operators.push(token);
        }

        while (!operators.isEmpty()) {
            String operator = operators.pop();
            if ("(".equals(operator)) {
                throw new IllegalArgumentException("括号不匹配");
            }
            output.add(operator);
        }

        return output;
    }

    private BigDecimal evalPostfix(List<String> postfix) {
        Deque<BigDecimal> stack = new ArrayDeque<>();

        for (String token : postfix) {
            if (isNumber(token)) {
                stack.push(new BigDecimal(token, MATH_CONTEXT));
                continue;
            }

            if (stack.size() < 2) {
                throw new IllegalArgumentException("表达式格式不正确");
            }

            BigDecimal right = stack.pop();
            BigDecimal left = stack.pop();
            stack.push(apply(left, right, token));
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("表达式格式不正确");
        }

        return stack.pop();
    }

    private BigDecimal apply(BigDecimal left, BigDecimal right, String operator) {
        return switch (operator) {
            case "+" -> left.add(right, MATH_CONTEXT);
            case "-" -> left.subtract(right, MATH_CONTEXT);
            case "*" -> left.multiply(right, MATH_CONTEXT);
            case "/" -> {
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw new IllegalArgumentException("除数不能为 0");
                }
                yield left.divide(right, MATH_CONTEXT);
            }
            default -> throw new IllegalArgumentException("不支持的操作符: " + operator);
        };
    }

    private boolean isNumber(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int start = value.startsWith("-") ? 1 : 0;
        boolean hasDot = false;
        for (int index = start; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '.') {
                if (hasDot) {
                    return false;
                }
                hasDot = true;
                continue;
            }
            if (!Character.isDigit(current)) {
                return false;
            }
        }
        return start < value.length();
    }

    private boolean isOperator(String token) {
        return "+".equals(token) || "-".equals(token) || "*".equals(token) || "/".equals(token);
    }

    private int precedence(String token) {
        return ("+".equals(token) || "-".equals(token)) ? 1 : 2;
    }
}
