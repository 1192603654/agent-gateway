package com.agent.gateway.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * 阿里云百炼 (DashScope) 智能体执行器
 * 针对新版百炼模型（Qwen3, CosyVoice 等）进行专项优化，支持 OpenAI 兼容端点和特殊参数注入。
 */
@Slf4j
public class DashScopeAgentExecutor implements AgentExecutor {
    private final String name;
    private final String description;
    private final String apiKey;
    private final String endpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public DashScopeAgentExecutor(String name, String description, String apiKey, String endpoint) {
        this.name = name;
        this.description = description;
        this.apiKey = apiKey;
        // 默认指向百炼的 OpenAI 兼容端点
        this.endpoint = (endpoint == null || endpoint.isEmpty()) ? "https://dashscope.aliyuncs.com/compatible-mode/v1" : endpoint;
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getDescription() { return description; }

    @Override
    public String execute(String input, Map<String, Object> parameters) {
        try {
            String model = (String) parameters.getOrDefault("model", "qwen3-max");
            String targetEndpoint = endpoint;

            // 专项优化 1：CosyVoice 声音复刻/设计接口特殊参数处理
            if (model.contains("cosyvoice")) {
                parameters.put("target_model", model);
                parameters.put("model", "voice-enrollment");
                // 声音复刻通常使用专用端点，若未指定则通过兼容模式尝试
            }

            // 专项优化 2：根据模型名称识别是否需要多模态调用（针对 compatible-mode 自动路由）
            // 在 OpenAI 兼容模式下，DashScope 会根据 model 名称自动分发，通常无需手动切换端点

            java.util.HashMap<String, Object> body = new java.util.HashMap<>();
            body.put("model", parameters.getOrDefault("model", model));
            body.put("messages", java.util.List.of(
                Map.of("role", "user", "content", input)
            ));

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetEndpoint + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("DashScope API 错误: {}, {}", response.statusCode(), response.body());
                return "DashScope 调用失败: " + response.body();
            }

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("执行 DashScope 智能体失败", e);
            return "错误: " + e.getMessage();
        }
    }

    @Override
    public String getAgentType() { return "dashscope"; }
}
