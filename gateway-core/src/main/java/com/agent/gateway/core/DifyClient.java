package com.agent.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Builder
public class DifyClient {
    private final String apiKey;
    private final String endpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String chat(String query, String user, Map<String, Object> inputs) {
        try {
            Map<String, Object> body = Map.of(
                    "inputs", inputs,
                    "query", query,
                    "user", user,
                    "response_mode", "blocking"
            );

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/chat-messages"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Dify API error: " + response.body());
            }

            JsonNode node = objectMapper.readTree(response.body());
            return node.get("answer").asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to call Dify API", e);
        }
    }
}
