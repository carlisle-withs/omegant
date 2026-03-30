package com.example.agent.router;

import com.example.agent.core.AgentRequest;

/**
 * 意图识别服务接口
 */
public interface IntentRecognitionService {

    /**
     * 识别用户意图
     * @param request 用户请求
     * @return 识别的意图类型
     */
    IntentType recognize(AgentRequest request);

    /**
     * 识别意图并返回置信度
     * @param request 用户请求
     * @return 意图识别结果
     */
    IntentRecognitionResult recognizeWithConfidence(AgentRequest request);
}
