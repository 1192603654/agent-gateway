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
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Slf4j
public class ConfigController {
    private final SystemConfigRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

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

            // If key or url is missing, try to load from saved config
            SystemConfig saved = repository.findById("ORCHESTRATOR_MODEL").orElse(null);
            if (saved != null && provider.equals(saved.getProvider())) {
                if (url == null || url.isEmpty()) url = saved.getBaseUrl();
                if (key == null || key.isEmpty()) key = saved.getApiKey();
            }

            if (url == null || url.isEmpty()) {
                if ("openai".equals(provider)) url = "https://api.openai.com/v1";
                else if ("zhipu".equals(provider)) url = "https://open.bigmodel.cn/api/paas/v4/";
                else if ("dashscope".equals(provider)) url = "https://dashscope.aliyuncs.com/api/v1";
            }

            if (key == null || key.isEmpty()) return models;

            String fetchUrl = url.endsWith("/") ? url + "models" : url + "/models";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fetchUrl))
                    .header("Authorization", "Bearer " + key)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode data = root.path("data");
                if (data.isArray()) {
                    for (JsonNode node : data) {
                        String id = node.path("id").asText();
                        if (id != null && !id.isEmpty()) models.add(id);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch models for provider {}", provider, e);
        }
        return models;
    }
}
