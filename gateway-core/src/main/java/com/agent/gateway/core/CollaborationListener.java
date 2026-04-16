package com.agent.gateway.core;

/**
 * 协作过程监听器
 * 用于接收协作过程中的各种回调事件
 */
public interface CollaborationListener {
    /**
     * 步骤开始执行
     */
    void onStepStart(String agentName, int step);

    /**
     * 接收步骤产生的流式数据块
     */
    void onStepChunk(String agentName, Object data);

    /**
     * 步骤执行完成
     */
    void onStepComplete(String agentName, String result);

    /**
     * 协作彻底完成
     * @param finalResult 最终聚合结果
     */
    void onComplete(String finalResult);

    /**
     * 接收元数据（如会话 ID 等）
     */
    default void onMetadata(String key, Object value) {}

    /**
     * 协作发生异常
     */
    void onError(String message);
}
