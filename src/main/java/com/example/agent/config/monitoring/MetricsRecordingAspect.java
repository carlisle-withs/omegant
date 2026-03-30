package com.example.agent.config.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 指标记录切面 - 自动记录方法执行指标
 */
@Aspect
@Component
public class MetricsRecordingAspect {

    private static final Logger log = LoggerFactory.getLogger(MetricsRecordingAspect.class);

    private final AgentMetrics agentMetrics;

    public MetricsRecordingAspect(AgentMetrics agentMetrics) {
        this.agentMetrics = agentMetrics;
    }

    @Pointcut("execution(* com.example.agent.service.AgentService.execute(..))")
    public void agentExecutePointcut() {}

    @Around("agentExecutePointcut()")
    public Object recordAgentExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        agentMetrics.recordRequest();

        try {
            Object result = joinPoint.proceed();
            agentMetrics.recordSuccess();
            return result;
        } catch (Exception e) {
            agentMetrics.recordFailure();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            agentMetrics.recordRequestDuration(duration);
            log.debug("Agent执行耗时: {}ms", duration);
        }
    }

    @Pointcut("execution(* com.example.agent.tools.Tool.execute(..))")
    public void toolExecutePointcut() {}

    @Around("toolExecutePointcut()")
    public Object recordToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        agentMetrics.recordToolCall();

        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            agentMetrics.recordToolExecutionDuration(duration);
            log.debug("工具 {} 执行耗时: {}ms", joinPoint.getSignature().getName(), duration);
        }
    }
}
