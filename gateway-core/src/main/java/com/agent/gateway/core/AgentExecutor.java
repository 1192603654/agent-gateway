package com.agent.gateway.core;

import java.util.Map;

/**
 * 智能体执行器接口
 * 所有的子智能体架构（如 Dify, OpenClaw）都需要实现此接口以接入网关。
 */
public interface AgentExecutor {
    /**
     * 获取智能体名称
     */
    String getName();

    /**
     * 获取智能体描述（用于编排模型识别其功能）
     */
    String getDescription();

    /**
     * 执行智能体（同步调用）
     * @param input 用户输入
     * @param parameters LLM 提取的参数
     * @return 执行结果
     */
    String execute(String input, Map<String, Object> parameters);

    /**
     * 流式执行智能体
     * @param input 用户输入
     * @param parameters LLM 提取的参数
     * @param chunkConsumer 接收流式数据块的回调，数据块可以是 String 或结构化的 JsonNode
     */
    default void executeStream(String input, Map<String, Object> parameters, java.util.function.Consumer<Object> chunkConsumer) {
        String result = execute(input, parameters);
        if (chunkConsumer != null) chunkConsumer.accept(result);
    }

    /**
     * 获取智能体类型 (如 "dify", "openclaw")
     */
    String getAgentType();
}
