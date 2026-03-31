package com.agent.gateway.core;

import java.util.Map;

public class AgentFactory {
    public static AgentExecutor create(String name, String type, String endpoint, String apiKey) {
        if ("dify".equalsIgnoreCase(type)) {
            return new DifyAgentExecutor(name, apiKey, endpoint);
        }
        throw new IllegalArgumentException("Unknown agent type: " + type);
    }
}
