package com.agent.gateway.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Builder
@Slf4j
public class OrchestratorService {
    private final ChatLanguageModel model;
    private final List<AgentExecutor> agents;

    public String process(String userInput) {
        // 1. Recognize intent and select agent
        String agentName = selectAgent(userInput);
        log.info("Selected agent: {}", agentName);

        if ("NONE".equals(agentName)) {
            return "I'm sorry, I couldn't identify a suitable agent for your request.";
        }

        // 2. Execute agent
        AgentExecutor executor = agents.stream()
                .filter(a -> a.getAgentType().equalsIgnoreCase(agentName) || a.getClass().getSimpleName().contains(agentName))
                .findFirst()
                .orElse(null);

        if (executor == null) {
            return "Selected agent " + agentName + " is not available.";
        }

        String result = executor.execute(userInput, Map.of());

        // 3. Verify fulfillment
        boolean finished = verifyFulfillment(userInput, result);
        log.info("Intent fulfilled: {}", finished);

        if (finished) {
            return result;
        } else {
            return "The agent executed but the intent might not be fully completed. Result: " + result;
        }
    }

    private String selectAgent(String userInput) {
        StringBuilder prompt = new StringBuilder("You are an intent recognizer. Available agents:\n");
        for (AgentExecutor agent : agents) {
            prompt.append("- ").append(agent.getClass().getSimpleName()).append(" (Type: ").append(agent.getAgentType()).append(")\n");
        }
        prompt.append("\nUser input: ").append(userInput);
        prompt.append("\nRespond with ONLY the name of the best agent or 'NONE'.");

        return model.generate(prompt.toString()).trim();
    }

    private boolean verifyFulfillment(String userInput, String result) {
        String prompt = String.format(
                "User Intent: %s\nAgent Result: %s\nIs the user intent fulfilled? Respond with ONLY 'YES' or 'NO'.",
                userInput, result
        );
        return model.generate(prompt).trim().equalsIgnoreCase("YES");
    }
}
