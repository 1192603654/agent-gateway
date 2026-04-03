package com.agent.gateway.core;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class CollaborationManagerTest {

    @Test
    void testCollaborate() {
        ChatLanguageModel mockModel = Mockito.mock(ChatLanguageModel.class);

        // Actually, let's use a named class for the test
        class MockAgent implements AgentExecutor {
            public String getName() { return "MockAgent"; }
            public String getDescription() { return "Mock description"; }
            public String execute(String input, java.util.Map<String, Object> parameters) { return "Mock result"; }
            public String getAgentType() { return "mock"; }
        }

        MockAgent agent = new MockAgent();

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
}
