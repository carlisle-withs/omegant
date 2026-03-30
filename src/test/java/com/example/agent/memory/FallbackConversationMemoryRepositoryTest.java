package com.example.agent.memory;

import com.example.agent.core.Message;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackConversationMemoryRepositoryTest {

    @Test
    void shouldUseFallbackWhenPrimaryFails() {
        ConversationMemoryRepository primary = new ConversationMemoryRepository() {
            @Override
            public void append(String sessionKey, Message message) {
                throw new IllegalStateException("redis unavailable");
            }

            @Override
            public List<Message> load(String sessionKey) {
                throw new IllegalStateException("redis unavailable");
            }

            @Override
            public void clear(String sessionKey) {
                throw new IllegalStateException("redis unavailable");
            }
        };

        InMemoryConversationMemoryRepository fallback = new InMemoryConversationMemoryRepository();
        FallbackConversationMemoryRepository repository = new FallbackConversationMemoryRepository(primary, fallback);

        repository.append("u::s", Message.user("hello"));

        assertThat(repository.load("u::s")).hasSize(1);

        repository.clear("u::s");

        assertThat(repository.load("u::s")).isEmpty();
    }
}
