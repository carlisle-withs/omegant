package com.example.agent.config;

import com.example.agent.core.LLMClient;
import com.example.agent.core.Tool;
import com.example.agent.llm.EmbeddingClient;
import com.example.agent.llm.FallbackLLMClient;
import com.example.agent.llm.NoopEmbeddingClient;
import com.example.agent.llm.OpenAiCompatibleEmbeddingClient;
import com.example.agent.llm.OpenAiCompatibleLLMClient;
import com.example.agent.llm.RuleBasedLLMClient;
import com.example.agent.memory.ConversationMemoryRepository;
import com.example.agent.memory.FallbackConversationMemoryRepository;
import com.example.agent.memory.InMemoryConversationMemoryRepository;
import com.example.agent.memory.RedisConversationMemoryRepository;
import com.example.agent.rag.ConversationRagService;
import com.example.agent.tools.CalculatorTool;
import com.example.agent.tools.KnowledgeTool;
import com.example.agent.tools.TimeTool;
import com.example.agent.tools.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.util.List;

@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentConfiguration {

    @Bean
    public Tool calculatorTool() {
        return new CalculatorTool();
    }

    @Bean
    public Tool timeTool() {
        return new TimeTool();
    }

    @Bean
    public Tool knowledgeTool(ConversationRagService conversationRagService) {
        return new KnowledgeTool(conversationRagService);
    }

    @Bean
    public ToolRegistry toolRegistry(List<Tool> tools) {
        return new ToolRegistry(tools);
    }

    @Bean
    public EmbeddingClient embeddingClient(AgentProperties agentProperties) {
        if (agentProperties.getLlm().isEnabled() && StringUtils.hasText(agentProperties.getLlm().getApiKey())) {
            return new OpenAiCompatibleEmbeddingClient(agentProperties);
        }
        return new NoopEmbeddingClient();
    }

    @Bean
    public LLMClient llmClient(
            AgentProperties agentProperties,
            ConversationRagService conversationRagService,
            ObjectMapper objectMapper
    ) {
        LLMClient fallbackClient = new RuleBasedLLMClient();
        if (agentProperties.getLlm().isEnabled() && StringUtils.hasText(agentProperties.getLlm().getApiKey())) {
            return new FallbackLLMClient(
                    new OpenAiCompatibleLLMClient(agentProperties, conversationRagService, objectMapper),
                    fallbackClient
            );
        }
        return fallbackClient;
    }

    @Bean
    public ConversationMemoryRepository conversationMemoryRepository(
            AgentProperties agentProperties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper
    ) {
        String provider = agentProperties.getSession().getProvider();
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        ConversationMemoryRepository inMemoryRepository = new InMemoryConversationMemoryRepository();
        if ("redis".equalsIgnoreCase(provider) && redisTemplate != null) {
            return new FallbackConversationMemoryRepository(
                    new RedisConversationMemoryRepository(redisTemplate, objectMapper, agentProperties),
                    inMemoryRepository
            );
        }
        return inMemoryRepository;
    }
}
