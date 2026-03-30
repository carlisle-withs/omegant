package com.example.agent.router;

/**
 * 意图识别结果
 */
public record IntentRecognitionResult(
        IntentType type,
        double confidence,
        String reason
) {
    public static IntentRecognitionResult of(IntentType type, double confidence, String reason) {
        return new IntentRecognitionResult(type, confidence, reason);
    }

    public static IntentRecognitionResult of(IntentType type) {
        return new IntentRecognitionResult(type, 1.0, "");
    }
}
