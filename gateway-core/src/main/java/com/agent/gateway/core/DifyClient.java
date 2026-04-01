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
        return chat(query, user, inputs, null);
    }

    public String chat(String query, String user, Map<String, Object> inputs, String conversationId) {
        final StringBuilder fullAnswer = new StringBuilder();
        chatStream(query, user, inputs, conversationId, chunk -> fullAnswer.append(chunk));
        return fullAnswer.toString();
    }

    public void chatStream(String query, String user, Map<String, Object> inputs, String conversationId, java.util.function.Consumer<String> chunkConsumer) {
        try {
            java.util.HashMap<String, Object> body = new java.util.HashMap<>();
            body.put("inputs", inputs != null ? inputs : Map.of());
            body.put("query", query);
            body.put("user", user);
            body.put("response_mode", "streaming");
            if (conversationId != null && !conversationId.isEmpty()) {
                body.put("conversation_id", conversationId);
            }

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + "/chat-messages"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Dify API error code: " + response.statusCode());
            }

            try (java.util.Scanner scanner = new java.util.Scanner(response.body(), "UTF-8")) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("data:")) {
                        JsonNode node = objectMapper.readTree(line.substring(5).trim());
                        if (node.has("event") && "message".equals(node.get("event").asText())) {
                            String answer = node.get("answer").asText();
                            if (chunkConsumer != null) chunkConsumer.accept(answer);
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to call Dify API", e);
        }
    }
}
