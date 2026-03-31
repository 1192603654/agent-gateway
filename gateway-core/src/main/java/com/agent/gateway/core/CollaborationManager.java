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

    public String collaborate(String userInput) {
        StringBuilder conversationHistory = new StringBuilder("User: ").append(userInput).append("\n");
        String currentResult = "";
        List<String> executedAgents = new ArrayList<>();

        for (int i = 0; i < 3; i++) { // Max 3 steps for simplicity
            String nextAction = selectNextAction(conversationHistory.toString(), executedAgents);
            if (nextAction.equals("FINISH") || nextAction.equals("NONE")) {
                break;
            }

            AgentExecutor executor = findExecutor(nextAction);
            if (executor == null) break;

            log.info("Collaborating step {}: calling agent {}", i + 1, nextAction);
            String result = executor.execute(userInput, Map.of("history", conversationHistory.toString()));
            conversationHistory.append("Agent (").append(nextAction).append("): ").append(result).append("\n");
            currentResult = result;
            executedAgents.add(nextAction);
        }

        return currentResult.isEmpty() ? "No agent could handle the request." : currentResult;
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
