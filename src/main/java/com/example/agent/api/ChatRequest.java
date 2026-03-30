package com.example.agent.api;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String userId,
        @NotBlank String sessionId,
        @NotBlank String message
) {
}
