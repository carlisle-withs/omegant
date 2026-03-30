package com.example.agent.memory;

import com.example.agent.core.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryConversationMemoryRepository implements ConversationMemoryRepository {

    private final Map<String, List<Message>> sessions = new ConcurrentHashMap<>();

    @Override
    public void append(String sessionKey, Message message) {
        sessions.computeIfAbsent(sessionKey, ignored -> new CopyOnWriteArrayList<>()).add(message);
    }

    @Override
    public List<Message> load(String sessionKey) {
        return new ArrayList<>(sessions.getOrDefault(sessionKey, List.of()));
    }

    @Override
    public void clear(String sessionKey) {
        sessions.remove(sessionKey);
    }
}
