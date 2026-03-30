package com.example.agent.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAnswerArchitectureQuestionThroughKnowledgeTool() throws Exception {
        mockMvc.perform(post("/api/v1/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":"u-1",
                                  "sessionId":"s-1",
                                  "message":"请介绍企业级 agent 的核心架构"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.agentId").value("architecture-agent"))
                .andExpect(jsonPath("$.finishReason").value("DONE"))
                .andExpect(jsonPath("$.toolCalls[0].name").value("knowledge"))
                .andExpect(jsonPath("$.content").isNotEmpty());
    }

    @Test
    void shouldCalculateExpression() throws Exception {
        mockMvc.perform(post("/api/v1/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":"u-2",
                                  "sessionId":"s-2",
                                  "message":"帮我计算 12 * (3 + 2)"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value("calculator-agent"))
                .andExpect(jsonPath("$.toolCalls[0].name").value("calculator"))
                .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("60")));
    }
}
