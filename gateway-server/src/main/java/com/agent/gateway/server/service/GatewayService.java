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
        if (config == null || config.getApiKey() == null) {
            // Fallback or default
            return OpenAiChatModel.withApiKey("demo");
        }

        String provider = config.getProvider() != null ? config.getProvider().toLowerCase() : "openai";

        switch (provider) {
            case "zhipu":
                return ZhipuAiChatModel.builder()
                        .apiKey(config.getApiKey())
                        .build();
            case "dashscope":
                return QwenChatModel.builder()
                        .apiKey(config.getApiKey())
                        .modelName(config.getModelName() != null && !config.getModelName().isEmpty() ? config.getModelName() : "qwen-turbo")
                        .build();
            case "openai":
            default:
                return OpenAiChatModel.builder()
                        .apiKey(config.getApiKey())
                        .baseUrl(config.getBaseUrl() != null && !config.getBaseUrl().isEmpty() ? config.getBaseUrl() : "https://api.openai.com/v1")
                        .modelName(config.getModelName() != null && !config.getModelName().isEmpty() ? config.getModelName() : "gpt-4")
                        .build();
        }
    }

    public void processStream(String query, Map<String, Object> params, CollaborationListener listener) {
        ChatLanguageModel model = getModel();
        List<AgentConfig> configs = repository.findAll();
        List<AgentExecutor> executors = configs.stream()
                .map(c -> AgentFactory.create(c.getName(), c.getType(), c.getEndpoint(), c.getApiKey()))
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
                .map(c -> AgentFactory.create(c.getName(), c.getType(), c.getEndpoint(), c.getApiKey()))
                .collect(Collectors.toList());

        CollaborationManager collaborationManager = CollaborationManager.builder()
                .orchestratorModel(model)
                .agents(executors)
                .build();

        return collaborationManager.collaborate(query);
    }
}
