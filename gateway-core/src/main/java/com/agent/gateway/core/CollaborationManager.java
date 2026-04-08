package com.agent.gateway.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 协作管理器 (Orchestrator)
 * 负责中心模型的意图识别、多智能体调度及协作流程管理。
 */
@Builder
@Slf4j
public class CollaborationManager {
    private final ChatLanguageModel orchestratorModel; // 中心编排模型
    private final List<AgentExecutor> agents;        // 已注册的智能体执行器列表

    /**
     * 开始协作流程
     * @param userInput 用户原始输入
     * @param params 初始参数
     * @param listener 协作过程监听器，用于处理流式输出和进度回调
     */
    public void collaborate(String userInput, Map<String, Object> params, CollaborationListener listener) {
        StringBuilder conversationHistory = new StringBuilder("User: ").append(userInput).append("\n");
        String currentResult = "";
        List<String> executedAgents = new ArrayList<>();

        try {
            // 最多执行 5 轮，防止死循环
            for (int i = 0; i < 5; i++) {
                // 1. 调用中心模型决定下一步动作
                String nextAction = selectNextAction(conversationHistory.toString(), executedAgents);
                if (nextAction.equals("FINISH") || nextAction.equals("NONE")) {
                    break;
                }

                // 2. 如果中心模型决定直接回答（如系统介绍、Agent 列表查询等）
                if (nextAction.equals("DIRECT_ANSWER")) {
                    if (listener != null) listener.onStepStart("Orchestrator", i + 1);
                    String response = generateDirectResponse(userInput, conversationHistory.toString());
                    if (listener != null) {
                        listener.onStepChunk("Orchestrator", response);
                        listener.onStepComplete("Orchestrator", response);
                    }
                    currentResult = response;
                    conversationHistory.append("Orchestrator: ").append(response).append("\n");
                    break;
                }

                // 3. 提取目标智能体名称和 AI 生成的参数
                String agentName = extractAgentName(nextAction);
                Map<String, Object> aiParams = extractParameters(nextAction);

                AgentExecutor executor = findExecutor(agentName);
                if (executor == null) {
                    log.warn("未找到指定的智能体执行器: {}", agentName);
                    break;
                }

                log.info("开始协作步骤 {}: 调用智能体 {}", i + 1, agentName);
                if (listener != null) listener.onStepStart(agentName, i + 1);

                // 合并初始参数、AI 提取的参数以及对话历史
                Map<String, Object> execParams = new java.util.HashMap<>(params != null ? params : Map.of());
                execParams.putAll(aiParams);
                execParams.put("history", conversationHistory.toString());

                StringBuilder stepResult = new StringBuilder();
                String finalNextAction = agentName;

                // 4. 执行智能体（流式输出）
                executor.executeStream(userInput, execParams, data -> {
                    if (data instanceof String s) {
                        stepResult.append(s);
                    } else if (data instanceof com.fasterxml.jackson.databind.JsonNode node) {
                        // 特殊处理 Dify 等返回的 JSON 结构，提取文本回答
                        if ("message".equals(node.path("event").asText())) {
                            stepResult.append(node.path("answer").asText());
                        }
                    }
                    if (listener != null) listener.onStepChunk(finalNextAction, data);
                });

                String result = stepResult.toString();
                conversationHistory.append("Agent (").append(agentName).append("): ").append(result).append("\n");
                currentResult = result;
                executedAgents.add(agentName);

                if (listener != null) listener.onStepComplete(agentName, result);
            }

            String finalRes = currentResult.isEmpty() ? "网关无法处理该请求。" : currentResult;
            if (listener != null) listener.onComplete(finalRes);
        } catch (Exception e) {
            log.error("协作流程执行异常", e);
            if (listener != null) listener.onError(e.getMessage());
        }
    }

    public void collaborate(String userInput, CollaborationListener listener) {
        collaborate(userInput, Map.of(), listener);
    }

    /**
     * 同步调用协作流程
     */
    public String collaborate(String userInput) {
        final String[] finalResult = new String[1];
        collaborate(userInput, new CollaborationListener() {
            @Override public void onStepStart(String agentName, int step) {}
            @Override public void onStepChunk(String agentName, Object data) {}
            @Override public void onStepComplete(String agentName, String result) {}
            @Override public void onComplete(String res) { finalResult[0] = res; }
            @Override public void onError(String msg) { finalResult[0] = "Error: " + msg; }
        });
        return finalResult[0];
    }

    /**
     * 调用中心模型进行决策
     */
    private String selectNextAction(String history, List<String> executed) {
        StringBuilder prompt = new StringBuilder("你是一个多智能体网关的中心调度器。\n");
        prompt.append("系统信息：这是一个基于 Java 开发的嵌入式网关，支持 Dify 和 OpenClaw 智能体。\n");
        prompt.append("你的职责：根据用户意图，协调不同的智能体完成任务。如果问题涉及系统身份、架构或 Agent 列表，请直接回答。\n\n");

        prompt.append("当前可用的智能体列表：\n");
        for (AgentExecutor agent : agents) {
            prompt.append("- ").append(agent.getName())
                  .append(" (类型: ").append(agent.getAgentType()).append(")\n")
                  .append("  功能描述: ").append(agent.getDescription()).append("\n");
        }

        prompt.append("\n输出指令：\n");
        prompt.append("1. 如果你可以直接回答（如介绍自己、系统架构、列出 Agent），请回复 'DIRECT_ANSWER'。\n");
        prompt.append("2. 如果需要调用 Agent，请分析其描述决定是否需要参数。\n");
        prompt.append("3. 如果需要参数，请以 JSON 格式回复: {\"agent\": \"AGENT_NAME\", \"parameters\": {\"key\": \"value\"}}。\n");
        prompt.append("4. 如果不需要参数，请仅回复智能体名称。\n");
        prompt.append("5. 如果任务已完成，请回复 'FINISH'。\n");

        prompt.append("\n已执行过的智能体: ").append(executed);
        prompt.append("\n对话历史：\n").append(history);
        prompt.append("\n请给出下一步动作（仅回复智能体名称、'DIRECT_ANSWER' 或 'FINISH'）：");

        return orchestratorModel.generate(prompt.toString()).trim();
    }

    /**
     * 生成中心模型的直接回答
     */
    private String generateDirectResponse(String userInput, String history) {
        StringBuilder prompt = new StringBuilder("你是一个多智能体网关的中心调度器。\n");
        prompt.append("系统信息：Java 嵌入式网关，Sub-Agent 模式，支持 Dify/OpenClaw。\n");
        prompt.append("技术架构：Java 21, Spring Boot, LangChain4j, JPA/H2 数据库。\n\n");

        prompt.append("当前配置的智能体：\n");
        if (agents.isEmpty()) {
            prompt.append("(暂未配置)\n");
        } else {
            for (AgentExecutor agent : agents) {
                prompt.append("- 名称: ").append(agent.getName()).append(", 类型: ").append(agent.getAgentType()).append("\n");
            }
        }

        prompt.append("\n用户提问: ").append(userInput);
        prompt.append("\n对话历史: ").append(history);
        prompt.append("\n\n请用中文提供准确且有帮助的直接回答。");

        return orchestratorModel.generate(prompt.toString());
    }

    private AgentExecutor findExecutor(String name) {
        return agents.stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从模型回复中提取智能体名称
     */
    private String extractAgentName(String nextAction) {
        String jsonStr = cleanJsonString(nextAction);
        if (jsonStr.startsWith("{")) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonStr);
                return node.path("agent").asText();
            } catch (Exception e) {
                log.warn("无法解析智能体 JSON 指令: {}", jsonStr);
            }
        }
        return nextAction.trim();
    }

    /**
     * 从模型回复中提取参数 Map
     */
    private Map<String, Object> extractParameters(String nextAction) {
        String jsonStr = cleanJsonString(nextAction);
        if (jsonStr.startsWith("{")) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonStr);
                com.fasterxml.jackson.databind.JsonNode paramsNode = node.path("parameters");
                if (paramsNode.isObject()) {
                    return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(paramsNode, Map.class);
                }
            } catch (Exception e) {
                log.warn("无法解析参数 JSON 指令: {}", jsonStr);
            }
        }
        return Map.of();
    }

    /**
     * 清理 LLM 输出的 JSON 字符串（处理 Markdown 代码块包裹的情况）
     */
    private String cleanJsonString(String input) {
        String result = input.trim();
        if (result.contains("```json")) {
            result = result.substring(result.indexOf("```json") + 7);
            if (result.contains("```")) {
                result = result.substring(0, result.indexOf("```"));
            }
        } else if (result.contains("```")) {
            result = result.substring(result.indexOf("```") + 3);
            if (result.contains("```")) {
                result = result.substring(0, result.indexOf("```"));
            }
        }
        return result.trim();
    }
}
