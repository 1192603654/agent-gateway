package com.agent.gateway.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CollaborationManagerTest {

    @Test
    void testCollaborate() {
        ChatLanguageModel mockModel = Mockito.mock(ChatLanguageModel.class);

        class MockAgent implements AgentExecutor {
            public String getName() { return "MockAgent"; }
            public String getDescription() { return "Mock description"; }
            public String execute(String input, Map<String, Object> parameters) { return "Mock result"; }
            public String getAgentType() { return "mock"; }
        }

        MockAgent agent = new MockAgent();

        // 1st call: selectNextAction -> "MockAgent"
        // 2nd call: selectNextAction -> "FINISH"
        when(mockModel.generate(anyString()))
                .thenReturn("MockAgent")
                .thenReturn("FINISH");

        CollaborationManager manager = CollaborationManager.builder()
                .orchestratorModel(mockModel)
                .agents(List.of(agent))
                .build();

        String result = manager.collaborate("Hello");
        assertEquals("Mock result", result);
    }

    @Test
    void testDirectAnswer() {
        ChatLanguageModel mockModel = Mockito.mock(ChatLanguageModel.class);

        // 1st call: selectNextAction -> "DIRECT_ANSWER"
        // 2nd call: generateDirectResponse -> "Directly answering"
        when(mockModel.generate(anyString()))
                .thenReturn("DIRECT_ANSWER")
                .thenReturn("Directly answering");

        CollaborationManager manager = CollaborationManager.builder()
                .orchestratorModel(mockModel)
                .agents(List.of())
                .build();

        String result = manager.collaborate("Who are you?");
        assertEquals("Directly answering", result);
    }
}
