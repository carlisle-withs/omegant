package com.example.agent.core;

import java.util.List;

public interface Memory {
    void add(Message message);

    List<Message> messages();

    void clear();
}
