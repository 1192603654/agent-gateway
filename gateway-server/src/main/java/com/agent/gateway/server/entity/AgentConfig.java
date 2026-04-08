package com.agent.gateway.server.entity;

import jakarta.persistence.Entity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class AgentConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type; // e.g., "dify"
    private String endpoint;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;
    private String description;
}
