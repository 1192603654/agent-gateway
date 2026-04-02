package com.agent.gateway.core;

import lombok.RequiredArgsConstructor;

import java.util.Map;

public class DifyAgentExecutor implements AgentExecutor {
    private final String name;
    private final String apiKey;
    private final String endpoint;

    public DifyAgentExecutor(String name, String apiKey, String endpoint) {
        this.name = name;
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String execute(String input, Map<String, Object> parameters) {
        final StringBuilder sb = new StringBuilder();
        executeStream(input, parameters, data -> {
            if (data instanceof String s) {
                sb.append(s);
            } else if (data instanceof com.fasterxml.jackson.databind.JsonNode node) {
                if ("message".equals(node.path("event").asText())) {
                    sb.append(node.path("answer").asText());
                }
            }
        });
        return sb.toString();
    }

    @Override
    public void executeStream(String input, Map<String, Object> parameters, java.util.function.Consumer<Object> chunkConsumer) {
        DifyClient client = DifyClient.builder()
                .apiKey(apiKey)
                .endpoint(endpoint)
                .build();

        String user = (String) parameters.getOrDefault("user", "gateway-user");
        String conversationId = (String) parameters.get("conversation_id");
        Map<String, Object> inputs = (Map<String, Object>) parameters.getOrDefault("inputs", parameters);

        client.chatStream(input, user, inputs, conversationId, chunkConsumer);
    }

    @Override
    public String getAgentType() {
        return "dify";
    }
}
