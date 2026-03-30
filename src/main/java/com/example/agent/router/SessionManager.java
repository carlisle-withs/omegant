package com.example.agent.router;

import com.example.agent.core.Memory;
import com.example.agent.memory.ConversationMemoryRepository;
import com.example.agent.memory.RepositoryBackedMemory;
import org.springframework.stereotype.Component;

@Component
public class SessionManager {

    private final ConversationMemoryRepository conversationMemoryRepository;

    public SessionManager(ConversationMemoryRepository conversationMemoryRepository) {
        this.conversationMemoryRepository = conversationMemoryRepository;
    }

    public Memory getOrCreate(String userId, String sessionId) {
        return new RepositoryBackedMemory(buildKey(userId, sessionId), conversationMemoryRepository);
    }

    public void clear(String userId, String sessionId) {
        conversationMemoryRepository.clear(buildKey(userId, sessionId));
    }

    private String buildKey(String userId, String sessionId) {
        return userId + "::" + sessionId;
    }
}
