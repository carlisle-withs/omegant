package com.example.agent.memory;

import com.example.agent.core.Message;

import java.util.List;

public interface ConversationMemoryRepository {
    void append(String sessionKey, Message message);

    List<Message> load(String sessionKey);

    void clear(String sessionKey);
}
