package com.agent.gateway.core;

import java.util.Map;

public class AgentFactory {
    public static AgentExecutor create(String name, String description, String type, String endpoint, String apiKey) {
        if ("dify".equalsIgnoreCase(type)) {
            return new DifyAgentExecutor(name, description, apiKey, endpoint);
        } else if ("openclaw".equalsIgnoreCase(type)) {
            return new OpenClawAgentExecutor(name, description, endpoint, apiKey);
        }
        throw new IllegalArgumentException("Unknown agent type: " + type);
    }
}
