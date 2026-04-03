package com.agent.gateway.server.controller;

import com.agent.gateway.server.entity.AgentConfig;
import com.agent.gateway.server.repository.AgentConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {
    private final AgentConfigRepository repository;

    @GetMapping
    public List<AgentConfig> list() {
        return repository.findAll();
    }

    @PostMapping
    public AgentConfig create(@RequestBody AgentConfig config) {
        if (config.getId() != null) {
            AgentConfig existing = repository.findById(config.getId()).orElse(null);
            if (existing != null && (config.getApiKey() == null || config.getApiKey().trim().isEmpty())) {
                config.setApiKey(existing.getApiKey());
            }
        }
        return repository.save(config);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
