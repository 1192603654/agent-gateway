package com.agent.gateway.server.controller;

import com.agent.gateway.server.entity.SystemConfig;
import com.agent.gateway.server.repository.SystemConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Slf4j
public class ConfigController {
    private final SystemConfigRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @GetMapping("/orchestrator")
    public SystemConfig getOrchestratorConfig() {
        return repository.findById("ORCHESTRATOR_MODEL").orElse(new SystemConfig());
    }

    @PostMapping("/orchestrator")
    public SystemConfig saveOrchestratorConfig(@RequestBody SystemConfig config) {
        SystemConfig existing = repository.findById("ORCHESTRATOR_MODEL").orElse(null);
        if (existing != null && (config.getApiKey() == null || config.getApiKey().isEmpty())) {
            config.setApiKey(existing.getApiKey());
        }
        config.setConfigKey("ORCHESTRATOR_MODEL");
        return repository.save(config);
    }

    @GetMapping("/models")
    public List<String> getModels(@RequestParam("provider") String provider,
                                 @RequestParam(value = "baseUrl", required = false) String baseUrl,
                                 @RequestParam(value = "apiKey", required = false) String apiKey) {
        List<String> models = new ArrayList<>();
        try {
            String url = baseUrl;
            String key = apiKey;

            SystemConfig saved = repository.findById("ORCHESTRATOR_MODEL").orElse(null);
            if (saved != null && provider.equals(saved.getProvider())) {
                if (url == null || url.isEmpty()) url = saved.getBaseUrl();
                if (key == null || key.isEmpty()) key = saved.getApiKey();
            }

            if (url == null || url.isEmpty()) {
                if ("openai".equals(provider)) url = "https://api.openai.com/v1";
                else if ("zhipu".equals(provider)) url = "https://open.bigmodel.cn/api/paas/v4/";
                else if ("dashscope".equals(provider)) url = "https://dashscope.aliyuncs.com/compatible-mode/v1";
            }

            if (key == null || key.isEmpty()) {
                log.warn("供应商 {} 的 API Key 为空，跳过模型获取", provider);
                return models;
            }
            key = key.trim();

            String fetchUrl = url;
            if (fetchUrl.endsWith("/models")) {
            } else if (fetchUrl.endsWith("/")) {
                fetchUrl += "models";
            } else {
                fetchUrl += "/models";
            }

            log.info("开始从 {} 获取供应商 {} 的模型列表", fetchUrl, provider);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fetchUrl))
                    .header("Authorization", "Bearer " + key)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("供应商 {} 模型接口响应状态码: {}, 响应体: {}", provider, response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode data = null;
                if (root.has("data") && root.get("data").isArray()) {
                    data = root.get("data");
                } else if (root.has("output") && root.get("output").has("models") && root.get("output").get("models").isArray()) {
                    data = root.get("output").get("models");
                } else if (root.has("models") && root.get("models").isArray()) {
                    data = root.get("models");
                }

                if (data != null && data.isArray()) {
                    for (JsonNode node : data) {
                        String id = node.has("id") ? node.get("id").asText() :
                                   (node.has("model_id") ? node.get("model_id").asText() :
                                   (node.has("model_name") ? node.get("model_name").asText() : ""));
                        if (!id.isEmpty()) models.add(id);
                    }
                }
            } else {
                log.error("获取模型列表失败，状态码: {}, 响应内容: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("获取供应商 {} 的模型列表发生异常", provider, e);
        }
        return models;
    }
}
