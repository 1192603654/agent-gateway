package com.agent.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class OpenClawAgentExecutor implements AgentExecutor {
    private final String name;
    private final String endpoint; // This should be the base URL of the OpenAPI service
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String execute(String input, Map<String, Object> parameters) {
        log.info("Executing OpenClaw agent: {} with input: {}", name, input);

        try {
            // In a real implementation, we would parse the OpenAPI spec (OpenClaw format)
            // and dynamically call the correct endpoint.
            // For now, we'll assume a generic action endpoint as per OpenClaw tool standards.

            Map<String, Object> body = Map.of(
                "action", "process",
                "input", input,
                "parameters", parameters
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/action"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("OpenClaw API error: " + response.body());
            }

            JsonNode node = objectMapper.readTree(response.body());
            return node.has("output") ? node.get("output").asText() : response.body();

        } catch (Exception e) {
            log.error("Failed to call OpenClaw API", e);
            throw new RuntimeException("Failed to call OpenClaw API", e);
        }
    }

    @Override
    public String getAgentType() {
        return "openclaw";
    }
}
