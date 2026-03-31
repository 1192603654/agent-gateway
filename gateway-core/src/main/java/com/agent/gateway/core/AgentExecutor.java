package com.agent.gateway.core;

import java.util.Map;

public interface AgentExecutor {
    String execute(String input, Map<String, Object> parameters);
    String getAgentType();
}
