package com.agent.gateway.server.service;

import com.agent.gateway.core.AgentExecutor;
import com.agent.gateway.core.AgentFactory;
import com.agent.gateway.core.OrchestratorService;
import com.agent.gateway.server.entity.AgentConfig;
import com.agent.gateway.server.repository.AgentConfigRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
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

    public String process(String query) {
        List<AgentConfig> configs = repository.findAll();
        List<AgentExecutor> executors = configs.stream()
                .map(c -> AgentFactory.create(c.getType(), c.getEndpoint(), c.getApiKey()))
                .collect(Collectors.toList());

        ChatLanguageModel model = OpenAiChatModel.withApiKey(llmApiKey);

        OrchestratorService orchestrator = OrchestratorService.builder()
                .model(model)
                .agents(executors)
                .build();

        return orchestrator.process(query);
    }
}
