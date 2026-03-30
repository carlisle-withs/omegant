package com.example.agent.llm;

import com.example.agent.config.AgentProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private final AgentProperties agentProperties;
    private final RestClient restClient;

    public OpenAiCompatibleEmbeddingClient(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(agentProperties.getLlm().getBaseUrl()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + agentProperties.getLlm().getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public boolean available() {
        return agentProperties.getLlm().isEnabled()
                && !agentProperties.getLlm().getApiKey().isBlank()
                && !agentProperties.getLlm().getEmbeddingModel().isBlank();
    }

    @Override
    public List<Double> embed(String text) {
        if (!available() || text == null || text.isBlank()) {
            return List.of();
        }

        try {
            JsonNode response = restClient.post()
                    .uri("/v1/embeddings")
                    .body(java.util.Map.of(
                            "model", agentProperties.getLlm().getEmbeddingModel(),
                            "input", text
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || response.path("data").isEmpty()) {
                return List.of();
            }

            List<Double> embedding = new ArrayList<>();
            JsonNode values = response.path("data").get(0).path("embedding");
            for (JsonNode value : values) {
                embedding.add(value.asDouble());
            }
            return embedding;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String trimTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
