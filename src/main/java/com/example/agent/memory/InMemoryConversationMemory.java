package com.example.agent.memory;

import com.example.agent.core.Memory;
import com.example.agent.core.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryConversationMemory implements Memory {

    private final List<Message> messages = new CopyOnWriteArrayList<>();

    @Override
    public void add(Message message) {
        messages.add(message);
    }

    @Override
    public List<Message> messages() {
        return new ArrayList<>(messages);
    }

    @Override
    public void clear() {
        messages.clear();
    }
}
