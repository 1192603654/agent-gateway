package com.agent.gateway.server.repository;

import com.agent.gateway.server.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
}
