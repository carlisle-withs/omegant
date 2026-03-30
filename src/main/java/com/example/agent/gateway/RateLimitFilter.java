package com.example.agent.gateway;

import com.example.agent.config.AgentProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final InMemoryRateLimiter rateLimiter;

    public RateLimitFilter(AgentProperties agentProperties) {
        this.rateLimiter = new InMemoryRateLimiter(agentProperties.getGateway().getRequestsPerMinute(), Duration.ofMinutes(1));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String rateLimitKey = resolveKey(request);
        if (!rateLimiter.allow(rateLimitKey)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"message":"请求过于频繁，请稍后再试"}
                    """.trim());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            return userId;
        }
        return request.getRemoteAddr();
    }
}
