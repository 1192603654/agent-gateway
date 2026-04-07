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

/**
 * 系统配置控制器
 * 负责管理中心编排模型的供应商、模型名称、API Key 等。
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Slf4j
public class ConfigController {
    private final SystemConfigRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * 获取当前中心编排模型配置
     */
    @GetMapping("/orchestrator")
    public SystemConfig getOrchestratorConfig() {
        return repository.findById("ORCHESTRATOR_MODEL").orElse(new SystemConfig());
    }

    /**
     * 保存中心编排模型配置
     */
    @PostMapping("/orchestrator")
    public SystemConfig saveOrchestratorConfig(@RequestBody SystemConfig config) {
        SystemConfig existing = repository.findById("ORCHESTRATOR_MODEL").orElse(null);
        // 如果 API Key 为空，保留数据库中的原有 Key
        if (existing != null && (config.getApiKey() == null || config.getApiKey().isEmpty())) {
            config.setApiKey(existing.getApiKey());
        }
        config.setConfigKey("ORCHESTRATOR_MODEL");
        return repository.save(config);
    }

    /**
     * 动态获取指定供应商的模型列表
     */
    @GetMapping("/models")
    public List<String> getModels(@RequestParam("provider") String provider,
                                 @RequestParam(value = "baseUrl", required = false) String baseUrl,
                                 @RequestParam(value = "apiKey", required = false) String apiKey) {
        List<String> models = new ArrayList<>();
        try {
            String url = baseUrl;
            String key = apiKey;

            // 如果参数未提供，尝试从已保存的配置中读取
            SystemConfig saved = repository.findById("ORCHESTRATOR_MODEL").orElse(null);
            if (saved != null && provider.equals(saved.getProvider())) {
                if (url == null || url.isEmpty()) url = saved.getBaseUrl();
                if (key == null || key.isEmpty()) key = saved.getApiKey();
            }

            // 默认端点 fallback
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
            log.error("获取供应商 {} 的模型列表失败", provider, e);
        }
        return models;
    }
}
