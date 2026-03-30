package com.example.agent.rag;

public record RetrievedChunk(
        String content,
        double score
) {
}
