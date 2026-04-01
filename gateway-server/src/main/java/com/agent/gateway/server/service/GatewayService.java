package com.agent.gateway.server.service;

import com.agent.gateway.core.AgentExecutor;
import com.agent.gateway.core.AgentFactory;
import com.agent.gateway.core.CollaborationListener;
import com.agent.gateway.core.CollaborationManager;
import com.agent.gateway.server.entity.AgentConfig;
import com.agent.gateway.server.repository.AgentConfigRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GatewayService {
    private final AgentConfigRepository repository;

    @Value("${gateway.llm.apiKey:demo}")
    private String llmApiKey;

    private ChatLanguageModel model;

    @PostConstruct
    public void init() {
        this.model = OpenAiChatModel.withApiKey(llmApiKey);
    }

    public void processStream(String query, CollaborationListener listener) {
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
