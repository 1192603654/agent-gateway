package com.agent.gateway.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Builder
@Slf4j
public class CollaborationManager {
    private final ChatLanguageModel orchestratorModel;
    private final List<AgentExecutor> agents;

    public void collaborate(String userInput, Map<String, Object> params, CollaborationListener listener) {
        StringBuilder conversationHistory = new StringBuilder("User: ").append(userInput).append("\n");
        String currentResult = "";
        List<String> executedAgents = new ArrayList<>();

        try {
            for (int i = 0; i < 5; i++) { // Max 5 steps for complex collaboration
                String nextAction = selectNextAction(conversationHistory.toString(), executedAgents);
                if (nextAction.equals("FINISH") || nextAction.equals("NONE")) {
                    break;
                }

                if (nextAction.equals("DIRECT_ANSWER")) {
                    if (listener != null) listener.onStepStart("Orchestrator", i + 1);
                    String response = generateDirectResponse(userInput, conversationHistory.toString());
                    if (listener != null) {
                        listener.onStepChunk("Orchestrator", response);
                        listener.onStepComplete("Orchestrator", response);
                    }
                    currentResult = response;
                    conversationHistory.append("Orchestrator: ").append(response).append("\n");
                    break; // Direct answer usually finishes the task
                }

                String agentName = extractAgentName(nextAction);
                Map<String, Object> aiParams = extractParameters(nextAction);

                AgentExecutor executor = findExecutor(agentName);
                if (executor == null) break;

                log.info("Collaborating step {}: calling agent {}", i + 1, agentName);
                if (listener != null) listener.onStepStart(agentName, i + 1);

                Map<String, Object> execParams = new java.util.HashMap<>(params != null ? params : Map.of());
                execParams.putAll(aiParams);
                execParams.put("history", conversationHistory.toString());

                StringBuilder stepResult = new StringBuilder();
                String finalNextAction = agentName;
                executor.executeStream(userInput, execParams, data -> {
                    if (data instanceof String s) {
                        stepResult.append(s);
                    } else if (data instanceof com.fasterxml.jackson.databind.JsonNode node) {
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

            String finalRes = currentResult.isEmpty() ? "No agent could handle the request." : currentResult;
            if (listener != null) listener.onComplete(finalRes);
        } catch (Exception e) {
            log.error("Collaboration failed", e);
            if (listener != null) listener.onError(e.getMessage());
        }
    }

    public void collaborate(String userInput, CollaborationListener listener) {
        collaborate(userInput, Map.of(), listener);
    }

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

    private String selectNextAction(String history, List<String> executed) {
        StringBuilder prompt = new StringBuilder("You are the Central Orchestrator of the Sub-Agent Gateway.\n");
        prompt.append("System Info: This is an embedded multi-agent gateway developed in Java, supporting Dify/OpenClaw agents.\n");
        prompt.append("Your Role: Coordinate between agents or answer directly if it's about system identity, architecture, or listing agents.\n\n");

        prompt.append("Available agents:\n");
        for (AgentExecutor agent : agents) {
            prompt.append("- ").append(agent.getName())
                  .append(" (Type: ").append(agent.getAgentType()).append(")\n")
                  .append("  Description: ").append(agent.getDescription()).append("\n");
        }

        prompt.append("\nInstructions:\n");
        prompt.append("1. If you can answer the user's question directly (e.g., identity, system architecture, listing agents), respond with 'DIRECT_ANSWER'.\n");
        prompt.append("2. If an agent is needed, analyze its description to determine if parameters are required.\n");
        prompt.append("3. If parameters are needed, respond in JSON format: {\"agent\": \"AGENT_NAME\", \"parameters\": {\"key\": \"value\"}}.\n");
        prompt.append("4. If NO parameters are needed, respond with ONLY the agent name.\n");
        prompt.append("5. If the user intent is fulfilled, respond with 'FINISH'.\n");

        prompt.append("\nExecuted so far: ").append(executed);
        prompt.append("\nConversation History:\n").append(history);
        prompt.append("\nNext Action (Respond ONLY with Agent Name, 'DIRECT_ANSWER', or 'FINISH'):");

        return orchestratorModel.generate(prompt.toString()).trim();
    }

    private String generateDirectResponse(String userInput, String history) {
        StringBuilder prompt = new StringBuilder("You are the Central Orchestrator of the Sub-Agent Gateway.\n");
        prompt.append("System Info: Java-based Embedded Gateway, Sub-Agent mode, supports Dify/OpenClaw agents.\n");
        prompt.append("Architecture: Java 21, Spring Boot, LangChain4j for orchestration, JPA/H2 for persistence.\n\n");

        prompt.append("Currently configured agents:\n");
        if (agents.isEmpty()) {
            prompt.append("(None)\n");
        } else {
            for (AgentExecutor agent : agents) {
                prompt.append("- Name: ").append(agent.getName()).append(", Type: ").append(agent.getAgentType()).append("\n");
            }
        }

        prompt.append("\nUser query: ").append(userInput);
        prompt.append("\nConversation history: ").append(history);
        prompt.append("\n\nPlease provide a helpful and accurate direct answer in Chinese.");

        return orchestratorModel.generate(prompt.toString());
    }

    private AgentExecutor findExecutor(String name) {
        return agents.stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private String extractAgentName(String nextAction) {
        if (nextAction.startsWith("{")) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(nextAction);
                return node.path("agent").asText();
            } catch (Exception e) {
                log.warn("Failed to parse agent JSON from Orchestrator: {}", nextAction);
            }
        }
        return nextAction;
    }

    private Map<String, Object> extractParameters(String nextAction) {
        if (nextAction.startsWith("{")) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(nextAction);
                com.fasterxml.jackson.databind.JsonNode paramsNode = node.path("parameters");
                if (paramsNode.isObject()) {
                    return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(paramsNode, Map.class);
                }
            } catch (Exception e) {
                log.warn("Failed to parse parameters JSON from Orchestrator: {}", nextAction);
            }
        }
        return Map.of();
    }
}
