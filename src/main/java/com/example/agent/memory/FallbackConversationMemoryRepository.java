package com.example.agent.memory;

import com.example.agent.core.Message;

import java.util.List;

public class FallbackConversationMemoryRepository implements ConversationMemoryRepository {

    private final ConversationMemoryRepository primary;
    private final ConversationMemoryRepository fallback;

    public FallbackConversationMemoryRepository(
            ConversationMemoryRepository primary,
            ConversationMemoryRepository fallback
    ) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public void append(String sessionKey, Message message) {
        try {
            primary.append(sessionKey, message);
        } catch (Exception exception) {
            fallback.append(sessionKey, message);
        }
    }

    @Override
    public List<Message> load(String sessionKey) {
        try {
            List<Message> messages = primary.load(sessionKey);
            if (!messages.isEmpty()) {
                return messages;
            }
        } catch (Exception exception) {
            return fallback.load(sessionKey);
        }
        return fallback.load(sessionKey);
    }

    @Override
    public void clear(String sessionKey) {
        try {
            primary.clear(sessionKey);
        } catch (Exception exception) {
            fallback.clear(sessionKey);
            return;
        }
        fallback.clear(sessionKey);
    }
}
