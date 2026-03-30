package com.example.agent.router;

/**
 * 用户意图类型枚举
 */
public enum IntentType {
    /**
     * 简单对话，无需复杂处理
     */
    CHAT,

    /**
     * 需要调用工具的任务
     */
    TOOL_CALL,

    /**
     * 需要多步骤规划的复杂任务
     */
    PLANNING,

    /**
     * 需要搜索/查询信息
     */
    QUERY,

    /**
     * 生成图片
     */
    IMAGE_GENERATION,

    /**
     * 未知意图
     */
    UNKNOWN
}
