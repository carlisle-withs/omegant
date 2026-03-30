package com.example.agent.rag;

import com.example.agent.config.AgentProperties;
import com.example.agent.core.Message;
import com.example.agent.knowledge.KnowledgeBase;
import com.example.agent.llm.NoopEmbeddingClient;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationRagServiceTest {

    @Test
    void shouldRetrieveKnowledgeFromLocalSegments() throws Exception {
        AgentProperties agentProperties = new AgentProperties();
        agentProperties.getRag().setEnabled(true);
        agentProperties.getRag().setTopK(2);

        ConversationRagService ragService = new ConversationRagService(
                new KnowledgeBase(new DefaultResourceLoader()),
                new NoopEmbeddingClient(),
                agentProperties
        );

        String result = ragService.search("企业级 agent 的核心架构是什么");

        assertThat(result).contains("检索到的知识片段");
        assertThat(result).contains("企业级 Agent");
    }

    @Test
    void shouldBuildConversationAwareQueryContext() throws Exception {
        AgentProperties agentProperties = new AgentProperties();
        agentProperties.getRag().setEnabled(true);
        agentProperties.getRag().setRewriteWithHistory(true);

        ConversationRagService ragService = new ConversationRagService(
                new KnowledgeBase(new DefaultResourceLoader()),
                new NoopEmbeddingClient(),
                agentProperties
        );

        String context = ragService.buildContext(java.util.List.of(
                Message.user("我们讨论一下企业级 agent"),
                Message.assistant("好的"),
                Message.user("那 memory 和 session 的区别是什么")
        ));

        assertThat(context).contains("Memory");
        assertThat(context).contains("Session");
    }
}
