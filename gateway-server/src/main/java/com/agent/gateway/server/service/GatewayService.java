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
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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

        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName() != null ? config.getModelName() : "gpt-4")
                .build();
    }

    public void processStream(String query, CollaborationListener listener) {
        ChatLanguageModel model = getModel();
        List<AgentConfig> configs = repository.findAll();
        List<AgentExecutor> executors = configs.stream()
                .map(c -> AgentFactory.create(c.getName(), c.getType(), c.getEndpoint(), c.getApiKey()))
                .collect(Collectors.toList());

        CollaborationManager collaborationManager = CollaborationManager.builder()
                .orchestratorModel(model)
                .agents(executors)
                .build();

        collaborationManager.collaborate(query, listener);
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
