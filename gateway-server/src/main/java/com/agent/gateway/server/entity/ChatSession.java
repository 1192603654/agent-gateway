package com.agent.gateway.server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天会话持久化实体
 */
@Entity
@Data
public class ChatSession {
    @Id
    private String sessionId;

    @Column(columnDefinition = "TEXT")
    private String historyContext = "";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_sessions", joinColumns = @JoinColumn(name = "session_id"))
    @MapKeyColumn(name = "agent_name")
    @Column(name = "conversation_id")
    private Map<String, String> agentConversationIds = new HashMap<>();

    private long lastActiveTime;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.lastActiveTime = System.currentTimeMillis();
    }
}
