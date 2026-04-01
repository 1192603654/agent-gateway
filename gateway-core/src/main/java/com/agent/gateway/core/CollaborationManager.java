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
            for (int i = 0; i < 3; i++) { // Max 3 steps for simplicity
                String nextAction = selectNextAction(conversationHistory.toString(), executedAgents);
                if (nextAction.equals("FINISH") || nextAction.equals("NONE")) {
                    break;
                }

                AgentExecutor executor = findExecutor(nextAction);
                if (executor == null) break;

                log.info("Collaborating step {}: calling agent {}", i + 1, nextAction);
                if (listener != null) listener.onStepStart(nextAction, i + 1);

                Map<String, Object> execParams = new java.util.HashMap<>(params != null ? params : Map.of());
                execParams.put("history", conversationHistory.toString());

                StringBuilder stepResult = new StringBuilder();
                String finalNextAction = nextAction;
                executor.executeStream(userInput, execParams, chunk -> {
                    stepResult.append(chunk);
                    if (listener != null) listener.onStepChunk(finalNextAction, chunk);
                });

                String result = stepResult.toString();
                conversationHistory.append("Agent (").append(nextAction).append("): ").append(result).append("\n");
                currentResult = result;
                executedAgents.add(nextAction);

                if (listener != null) listener.onStepComplete(nextAction, result);
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
            @Override public void onStepChunk(String agentName, String chunk) {}
            @Override public void onStepComplete(String agentName, String result) {}
            @Override public void onComplete(String res) { finalResult[0] = res; }
            @Override public void onError(String msg) { finalResult[0] = "Error: " + msg; }
        });
        return finalResult[0];
    }

    private String selectNextAction(String history, List<String> executed) {
        StringBuilder prompt = new StringBuilder("Based on the conversation history, select the next agent to call or 'FINISH' if the user intent is fulfilled.\n");
        prompt.append("Available agents:\n");
        for (AgentExecutor agent : agents) {
            prompt.append("- ").append(agent.getName()).append("\n");
        }
        prompt.append("\nExecuted so far: ").append(executed);
        prompt.append("\nConversation History:\n").append(history);
        prompt.append("\nRespond ONLY with the agent name or 'FINISH'.");

        return orchestratorModel.generate(prompt.toString()).trim();
    }

    private AgentExecutor findExecutor(String name) {
        return agents.stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
