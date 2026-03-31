package com.agent.gateway.server.controller;

import com.agent.gateway.server.service.GatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayController {
    private final GatewayService gatewayService;

    @PostMapping("/query")
    public Map<String, String> query(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        String result = gatewayService.process(query);
        return Map.of("result", result);
    }
}
