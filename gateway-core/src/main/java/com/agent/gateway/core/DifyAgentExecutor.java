package com.agent.gateway.core;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class DifyAgentExecutor implements AgentExecutor {
    private final String apiKey;
    private final String endpoint;

    @Override
    public String execute(String input, Map<String, Object> parameters) {
        DifyClient client = DifyClient.builder()
                .apiKey(apiKey)
                .endpoint(endpoint)
                .build();

        return client.chat(input, "gateway-user", parameters);
    }

    @Override
    public String getAgentType() {
        return "dify";
    }
}
