package com.example.agent.llm;

import java.util.List;

public class NoopEmbeddingClient implements EmbeddingClient {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public List<Double> embed(String text) {
        return List.of();
    }
}
