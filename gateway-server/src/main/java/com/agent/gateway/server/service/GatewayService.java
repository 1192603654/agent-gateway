package com.agent.gateway.server.service;

import com.agent.gateway.core.AgentExecutor;
import com.agent.gateway.core.AgentFactory;
import com.agent.gateway.core.CollaborationListener;
import com.agent.gateway.core.CollaborationManager;
import com.agent.gateway.server.entity.AgentConfig;
import com.agent.gateway.server.entity.SystemConfig;
import com.agent.gateway.server.repository.AgentConfigRepository;
import com.agent.gateway.server.repository.SystemConfigRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GatewayService {
    private final AgentConfigRepository repository;
    private final SystemConfigRepository systemConfigRepository;

    private ChatLanguageModel getModel() {
        SystemConfig config = systemConfigRepository.findById("ORCHESTRATOR_MODEL").orElse(null);
        if (config == null || config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            // Fallback or default
            return OpenAiChatModel.withApiKey("demo");
        }

        String apiKey = config.getApiKey().trim();
        validateApiKey(apiKey);

        String provider = config.getProvider() != null ? config.getProvider().toLowerCase() : "openai";

        switch (provider) {
            case "zhipu":
                return ZhipuAiChatModel.builder()
                        .apiKey(apiKey)
                        .build();
            case "dashscope":
                String modelName = config.getModelName() != null && !config.getModelName().isEmpty() ? config.getModelName() : "qwen3-max";
                // 优化：针对新一代 Qwen3 及多模态模型，使用 OpenAI 兼容模式连接，解决原生 SDK 的 URL 适配问题
                if (modelName.startsWith("qwen3") || modelName.contains("plus") || modelName.contains("max")) {
                    return OpenAiChatModel.builder()
                            .apiKey(apiKey)
                            .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                            .modelName(modelName)
                            .build();
                }
                return QwenChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build();
            case "openai":
            default:
                return OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(config.getBaseUrl() != null && !config.getBaseUrl().isEmpty() ? config.getBaseUrl() : "https://api.openai.com/v1")
                        .modelName(config.getModelName() != null && !config.getModelName().isEmpty() ? config.getModelName() : "gpt-4")
                        .build();
        }
    }

    private void validateApiKey(String apiKey) {
        for (int i = 0; i < apiKey.length(); i++) {
            char c = apiKey.charAt(i);
            if (c > 127) {
                throw new RuntimeException("API Key 包含非法字符 (0x" + Integer.toHexString(c) + ")，请确保密钥不包含中文字符或特殊格式。");
            }
        }
    }

    public void processStream(String query, Map<String, Object> params, CollaborationListener listener) {
        ChatLanguageModel model = getModel();
        List<AgentConfig> configs = repository.findAll();
        List<AgentExecutor> executors = configs.stream()
                .map(c -> {
                    String key = c.getApiKey() != null ? c.getApiKey().trim() : "";
                    return AgentFactory.create(c.getName(), c.getDescription(), c.getType(), c.getEndpoint(), key);
                })
                .collect(Collectors.toList());

        CollaborationManager collaborationManager = CollaborationManager.builder()
                .orchestratorModel(model)
                .agents(executors)
                .build();

        collaborationManager.collaborate(query, params, listener);
    }

    public String process(String query) {
        ChatLanguageModel model = getModel();
        List<AgentConfig> configs = repository.findAll();
        List<AgentExecutor> executors = configs.stream()
                .map(c -> {
                    String key = c.getApiKey() != null ? c.getApiKey().trim() : "";
                    return AgentFactory.create(c.getName(), c.getDescription(), c.getType(), c.getEndpoint(), key);
                })
                .collect(Collectors.toList());

        CollaborationManager collaborationManager = CollaborationManager.builder()
                .orchestratorModel(model)
                .agents(executors)
                .build();

        return collaborationManager.collaborate(query);
    }
}
