package com.example.agent.tracing;

import com.example.agent.core.*;
import com.example.agent.service.AgentExecutionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent 全链路追踪切面
 */
@Aspect
@Component
public class AgentTracingAspect {

    private static final Logger log = LoggerFactory.getLogger("agent.tracing");
    private final ObjectMapper objectMapper;

    public AgentTracingAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ========== AgentService.execute 全链路入口 ==========

    @Around("execution(* com.example.agent.service.AgentService.execute(..))")
    public Object traceAgentExecute(ProceedingJoinPoint joinPoint) throws Throwable {
        AgentRequest request = (AgentRequest) joinPoint.getArgs()[0];
        String traceId = initTrace();
        long startTime = System.currentTimeMillis();

        log.info("[{}] AGENT_REQUEST | userId={} | sessionId={} | message={}",
                traceId, request.userId(), request.sessionId(), truncate(request.userInput(), 200));

        try {
            AgentExecutionResult result = (AgentExecutionResult) joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.info("[{}] AGENT_RESPONSE | userId={} | agentId={} | response={} | toolCalls={} | duration={}ms",
                    traceId,
                    request.userId(),
                    result.agentId(),
                    truncate(result.response().content(), 300),
                    result.response().toolCalls().size(),
                    duration);

            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[{}] AGENT_ERROR | userId={} | error={} | duration={}ms",
                    traceId, request.userId(), t.getMessage(), duration);
            throw t;
        } finally {
            TraceContext.clear();
        }
    }

    // ========== LLM Client.chat 调用 ==========

    @Around("execution(* com.example.agent.core.LLMClient.chat(..))")
    public Object traceLlmChat(ProceedingJoinPoint joinPoint) throws Throwable {
        List<Message> messages = (List<Message>) joinPoint.getArgs()[0];
        List<Tool> tools = (List<Tool>) joinPoint.getArgs()[1];
        String traceId = TraceContext.getTraceId();
        long startTime = System.currentTimeMillis();

        log.info("[{}] LLM_REQUEST | messages={} | tools={}",
                traceId,
                messages.size(),
                tools.stream().map(Tool::name).collect(Collectors.joining(",")));

        try {
            LLMResponse response = (LLMResponse) joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            if (!response.toolCalls().isEmpty()) {
                log.info("[{}] LLM_TOOL_CALLS | tools={} | count={} | duration={}ms",
                        traceId,
                        response.toolCalls().stream()
                                .map(tc -> tc.name() + "(" + tc.id() + ")")
                                .collect(Collectors.joining(",")),
                        response.toolCalls().size(),
                        duration);
            } else {
                log.info("[{}] LLM_RESPONSE | content={} | duration={}ms",
                        traceId,
                        truncate(response.content(), 200),
                        duration);
            }

            return response;
        } catch (Throwable t) {
            log.error("[{}] LLM_ERROR | error={}", traceId, t.getMessage());
            throw t;
        }
    }

    // ========== Tool.execute 工具调用 ==========

    @Around("execution(* com.example.agent.tools.*.execute(..))")
    public Object traceToolExecute(ProceedingJoinPoint joinPoint) throws Throwable {
        Tool tool = (Tool) joinPoint.getTarget();
        Map<String, Object> params = (Map<String, Object>) joinPoint.getArgs()[0];
        String traceId = TraceContext.getTraceId();
        long startTime = System.currentTimeMillis();

        log.info("[{}] TOOL_CALL | name={} | params={}",
                traceId, tool.name(), toJson(params));

        try {
            ToolResult result = (ToolResult) joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.info("[{}] TOOL_RESULT | name={} | success={} | output={} | duration={}ms",
                    traceId,
                    tool.name(),
                    result.success(),
                    result.success() ? truncate(result.output(), 200) : truncate(result.error(), 200),
                    duration);

            return result;
        } catch (Throwable t) {
            log.error("[{}] TOOL_ERROR | name={} | error={}",
                    traceId, tool.name(), t.getMessage());
            throw t;
        }
    }

    // ========== 工具方法 ==========

    private String initTrace() {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        TraceContext.setTraceId(traceId);
        return traceId;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
