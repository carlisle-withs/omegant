package com.example.agent.llm;

import java.util.List;

public interface EmbeddingClient {
    boolean available();

    List<Double> embed(String text);
}
