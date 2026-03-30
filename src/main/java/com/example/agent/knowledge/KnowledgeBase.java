package com.example.agent.knowledge;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class KnowledgeBase {

    private final List<String> segments;

    public KnowledgeBase(ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:knowledge/agent-knowledge.txt");
        String text = resource.getContentAsString(StandardCharsets.UTF_8);
        this.segments = Arrays.stream(text.split("\\R\\R+"))
                .map(String::trim)
                .filter(segment -> !segment.isBlank())
                .toList();
    }

    public String search(String query) {
        return topSegments(query, 2).stream()
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
    }

    public List<String> segments() {
        return segments;
    }

    public List<String> topSegments(String query, int limit) {
        Set<String> keywords = tokenize(query);
        return segments.stream()
                .sorted(Comparator.comparingInt(segment -> -score(segment, keywords)))
                .limit(limit)
                .toList();
    }

    private int score(String segment, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return 0;
        }
        String normalized = segment.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String keyword : keywords) {
            if (normalized.contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    private Set<String> tokenize(String query) {
        return Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toSet());
    }
}
