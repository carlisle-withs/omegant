package com.example.agent.memory;

import com.example.agent.config.AgentProperties;
import com.example.agent.core.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

public class RedisConversationMemoryRepository implements ConversationMemoryRepository {

    private static final TypeReference<List<Message>> MESSAGE_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisConversationMemoryRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AgentProperties agentProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofMinutes(agentProperties.getSession().getTtlMinutes());
    }

    @Override
    public void append(String sessionKey, Message message) {
        List<Message> messages = load(sessionKey);
        messages.add(message);
        write(sessionKey, messages);
    }

    @Override
    public List<Message> load(String sessionKey) {
        String value = redisTemplate.opsForValue().get(sessionKey);
        if (value == null || value.isBlank()) {
            return new java.util.ArrayList<>();
        }
        try {
            return objectMapper.readValue(value, MESSAGE_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Redis 会话反序列化失败", exception);
        }
    }

    @Override
    public void clear(String sessionKey) {
        redisTemplate.delete(sessionKey);
    }

    private void write(String sessionKey, List<Message> messages) {
        try {
            redisTemplate.opsForValue().set(sessionKey, objectMapper.writeValueAsString(messages), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Redis 会话序列化失败", exception);
        }
    }
}
