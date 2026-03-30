package com.example.agent.api;

import com.example.agent.core.AgentRequest;
import com.example.agent.service.AgentExecutionResult;
import com.example.agent.service.AgentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest servletRequest) {
        AgentExecutionResult result = agentService.execute(new AgentRequest(
                request.userId(),
                request.sessionId(),
                request.message()
        ));
        return ResponseEntity.ok(new ChatResponse(
                String.valueOf(servletRequest.getAttribute(REQUEST_ID_ATTRIBUTE)),
                result.agentId(),
                result.response().content(),
                result.response().finishReason(),
                result.response().toolCalls()
        ));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearSession(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String sessionId
    ) {
        agentService.clearSession(userId, sessionId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "sessionId", sessionId,
                "cleared", true
        ));
    }
}
