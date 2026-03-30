package com.example.agent.memory;

import com.example.agent.core.Memory;
import com.example.agent.core.Message;

import java.util.List;

public class RepositoryBackedMemory implements Memory {

    private final String sessionKey;
    private final ConversationMemoryRepository repository;

    public RepositoryBackedMemory(String sessionKey, ConversationMemoryRepository repository) {
        this.sessionKey = sessionKey;
        this.repository = repository;
    }

    @Override
    public void add(Message message) {
        repository.append(sessionKey, message);
    }

    @Override
    public List<Message> messages() {
        return repository.load(sessionKey);
    }

    @Override
    public void clear() {
        repository.clear(sessionKey);
    }
}
