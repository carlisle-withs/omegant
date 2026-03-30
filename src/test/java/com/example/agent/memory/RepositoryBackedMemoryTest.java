package com.example.agent.memory;

import com.example.agent.core.Message;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryBackedMemoryTest {

    @Test
    void shouldPersistMessagesThroughRepository() {
        ConversationMemoryRepository repository = new InMemoryConversationMemoryRepository();
        RepositoryBackedMemory memory = new RepositoryBackedMemory("user::session", repository);

        memory.add(Message.user("你好"));
        memory.add(Message.assistant("你好，我是 agent"));

        assertThat(memory.messages()).hasSize(2);
        assertThat(memory.messages().get(0).content()).isEqualTo("你好");

        memory.clear();

        assertThat(memory.messages()).isEmpty();
    }
}
