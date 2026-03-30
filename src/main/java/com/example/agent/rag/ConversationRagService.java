package com.example.agent.rag;

import com.example.agent.config.AgentProperties;
import com.example.agent.core.Message;
import com.example.agent.core.MessageRole;
import com.example.agent.knowledge.KnowledgeBase;
import com.example.agent.llm.EmbeddingClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationRagService {

    private final KnowledgeBase knowledgeBase;
    private final EmbeddingClient embeddingClient;
    private final AgentProperties agentProperties;
    private final Map<String, List<Double>> embeddingCache = new ConcurrentHashMap<>();

    public ConversationRagService(
            KnowledgeBase knowledgeBase,
            EmbeddingClient embeddingClient,
            AgentProperties agentProperties
    ) {
        this.knowledgeBase = knowledgeBase;
        this.embeddingClient = embeddingClient;
        this.agentProperties = agentProperties;
    }

    public String buildContext(List<Message> conversation) {
        if (!agentProperties.getRag().isEnabled()) {
            return "";
        }
        String query = buildRetrievalQuery(conversation);
        if (query.isBlank()) {
            return "";
        }
        return format(retrieve(query));
    }

    public String search(String query) {
        return format(retrieve(query));
    }

    public List<RetrievedChunk> retrieve(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        if (embeddingClient.available()) {
            List<Double> queryEmbedding = embeddingClient.embed(query);
            if (!queryEmbedding.isEmpty()) {
                return retrieveByEmbeddings(queryEmbedding);
            }
        }

        return knowledgeBase.topSegments(query, agentProperties.getRag().getTopK()).stream()
                .map(segment -> new RetrievedChunk(segment, 1.0))
                .toList();
    }

    private List<RetrievedChunk> retrieveByEmbeddings(List<Double> queryEmbedding) {
        List<RetrievedChunk> retrievedChunks = new ArrayList<>();
        for (String segment : knowledgeBase.segments()) {
            List<Double> embedding = embeddingCache.computeIfAbsent(segment, embeddingClient::embed);
            if (embedding.isEmpty()) {
                continue;
            }
            double similarity = cosineSimilarity(queryEmbedding, embedding);
            if (similarity >= agentProperties.getRag().getSimilarityThreshold()) {
                retrievedChunks.add(new RetrievedChunk(segment, similarity));
            }
        }

        return retrievedChunks.stream()
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(agentProperties.getRag().getTopK())
                .toList();
    }

    private String buildRetrievalQuery(List<Message> conversation) {
        List<String> userMessages = conversation.stream()
                .filter(message -> message.role() == MessageRole.USER)
                .map(Message::content)
                .filter(content -> content != null && !content.isBlank())
                .toList();

        if (userMessages.isEmpty()) {
            return "";
        }

        String latest = userMessages.getLast();
        if (!agentProperties.getRag().isRewriteWithHistory() || userMessages.size() == 1) {
            return latest;
        }

        int fromIndex = Math.max(0, userMessages.size() - 3);
        return String.join(System.lineSeparator(), userMessages.subList(fromIndex, userMessages.size()));
    }

    private String format(List<RetrievedChunk> retrievedChunks) {
        if (retrievedChunks.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("以下是检索到的知识片段：");
        int index = 1;
        for (RetrievedChunk chunk : retrievedChunks) {
            builder.append(System.lineSeparator())
                    .append(index++)
                    .append(". ")
                    .append(chunk.content());
        }
        return builder.toString();
    }

    private double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left.size() != right.size() || left.isEmpty()) {
            return 0;
        }

        double dotProduct = 0;
        double leftNorm = 0;
        double rightNorm = 0;

        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index);
            double rightValue = right.get(index);
            dotProduct += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }

        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }

        return dotProduct / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
