package com.agent.gateway.server.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class SystemConfig {
    @Id
    private String configKey; // e.g., "ORCHESTRATOR_MODEL"

    private String provider;  // e.g., "openai"
    private String baseUrl;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;
    private String modelName;
}
