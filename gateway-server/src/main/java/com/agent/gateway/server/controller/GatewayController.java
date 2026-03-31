package com.agent.gateway.server.controller;

import com.agent.gateway.server.service.GatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Gateway API", description = "网关对外统一调用接口，支持第三方系统集成。")
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayController {
    private final GatewayService gatewayService;

    @Operation(summary = "提交意图请求", description = "第三方系统通过此接口提交用户意图，网关将自动识别、编排并执行相关智能体。")
    @ApiResponse(responseCode = "200", description = "执行完成，返回结果", content = @Content(schema = @Schema(example = "{\"result\": \"这是智能体执行后的回复内容\"}")))
    @PostMapping("/query")
    public Map<String, String> query(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        String result = gatewayService.process(query);
        return Map.of("result", result);
    }
}
