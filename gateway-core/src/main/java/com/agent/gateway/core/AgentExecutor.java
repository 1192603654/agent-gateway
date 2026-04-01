package com.agent.gateway.core;

import java.util.Map;

public interface AgentExecutor {
    String getName();
    String execute(String input, Map<String, Object> parameters);

    default void executeStream(String input, Map<String, Object> parameters, java.util.function.Consumer<String> chunkConsumer) {
        String result = execute(input, parameters);
        if (chunkConsumer != null) chunkConsumer.accept(result);
    }

    String getAgentType();
}
