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

/**
 * Dify 客户端
 * 负责与 Dify 的 chat-messages 接口进行通信，支持 SSE 流式解析。
 */
@Builder
@Slf4j
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
        chatStream(query, user, inputs, conversationId, chunk -> {
            if (chunk instanceof JsonNode node) {
                if ("message".equals(node.path("event").asText())) {
                    fullAnswer.append(node.path("answer").asText());
                }
            } else if (chunk instanceof String s) {
                fullAnswer.append(s);
            }
        });
        return fullAnswer.toString();
    }

    /**
     * 发起流式聊天请求
     */
    public void chatStream(String query, String user, Map<String, Object> inputs, String conversationId, java.util.function.Consumer<Object> chunkConsumer) {
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

            // 发起请求并获取响应流
            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Dify API 返回错误码: " + response.statusCode());
            }

            // 解析 SSE 数据流
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(response.body(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        if (data.isEmpty()) continue;
                        try {
                            JsonNode node = objectMapper.readTree(data);
                            if (chunkConsumer != null) {
                                chunkConsumer.accept(node);
                            }
                        } catch (Exception e) {
                            log.warn("无法解析 Dify SSE 数据: {}", data);
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("调用 Dify API 失败", e);
        }
    }
}
