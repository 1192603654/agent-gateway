package com.agent.gateway.server.repository;

import com.agent.gateway.server.entity.AgentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentConfigRepository extends JpaRepository<AgentConfig, Long> {
}
