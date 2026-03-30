package com.example.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private final Runtime runtime = new Runtime();
    private final Gateway gateway = new Gateway();
    private final Llm llm = new Llm();
    private final Rag rag = new Rag();
    private final Session session = new Session();

    public Runtime getRuntime() {
        return runtime;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public Llm getLlm() {
        return llm;
    }

    public Rag getRag() {
        return rag;
    }

    public Session getSession() {
        return session;
    }

    public static class Runtime {
        private int maxIterations = 6;

        public int getMaxIterations() {
            return maxIterations;
        }

        public void setMaxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
        }
    }

    public static class Gateway {
        private int requestsPerMinute = 60;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }

    public static class Llm {
        private boolean enabled;
        private String baseUrl = "https://api.openai.com";
        private String apiKey = "";
        private String chatModel = "gpt-4o-mini";
        private String embeddingModel = "text-embedding-3-small";
        private double temperature = 0.2;
        private int maxContextMessages = 12;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxContextMessages() {
            return maxContextMessages;
        }

        public void setMaxContextMessages(int maxContextMessages) {
            this.maxContextMessages = maxContextMessages;
        }
    }

    public static class Rag {
        private boolean enabled = true;
        private int topK = 4;
        private double similarityThreshold = 0.2;
        private boolean rewriteWithHistory = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        public boolean isRewriteWithHistory() {
            return rewriteWithHistory;
        }

        public void setRewriteWithHistory(boolean rewriteWithHistory) {
            this.rewriteWithHistory = rewriteWithHistory;
        }
    }

    public static class Session {
        private String provider = "memory";
        private long ttlMinutes = 120;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public long getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(long ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }
    }
}
