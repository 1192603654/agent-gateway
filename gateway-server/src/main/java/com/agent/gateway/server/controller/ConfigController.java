package com.agent.gateway.server.controller;

import com.agent.gateway.server.entity.SystemConfig;
import com.agent.gateway.server.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {
    private final SystemConfigRepository repository;

    @GetMapping("/orchestrator")
    public SystemConfig getOrchestratorConfig() {
        return repository.findById("ORCHESTRATOR_MODEL").orElse(new SystemConfig());
    }

    @PostMapping("/orchestrator")
    public SystemConfig saveOrchestratorConfig(@RequestBody SystemConfig config) {
        config.setConfigKey("ORCHESTRATOR_MODEL");
        return repository.save(config);
    }
}
