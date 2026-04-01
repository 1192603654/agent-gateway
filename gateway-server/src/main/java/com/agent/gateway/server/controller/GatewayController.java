package com.agent.gateway.server.controller;

import com.agent.gateway.core.CollaborationListener;
import com.agent.gateway.server.service.GatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Tag(name = "Gateway API", description = "网关对外统一调用接口，支持第三方系统集成。")
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayController {
    private final GatewayService gatewayService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Operation(summary = "提交意图请求 (SSE 流式)", description = "第三方系统通过此接口提交用户意图，网关将以服务器发送事件 (SSE) 的形式实时返回执行进度和最终结果。")
    @PostMapping(value = "/query", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter query(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout

        executor.execute(() -> {
            gatewayService.processStream(query, request, new CollaborationListener() {
                @Override
                public void onStepStart(String agentName, int step) {
                    sendEvent(emitter, "step_start", Map.of("agent", agentName, "step", step));
                }

                @Override
                public void onStepComplete(String agentName, String result) {
                    sendEvent(emitter, "step_complete", Map.of("agent", agentName, "result", result));
                }

                @Override
                public void onComplete(String finalResult) {
                    sendEvent(emitter, "final_result", Map.of("result", finalResult));
                    emitter.complete();
                }

                @Override
                public void onError(String message) {
                    sendEvent(emitter, "error", Map.of("message", message));
                    emitter.completeWithError(new RuntimeException(message));
                }
            });
        });

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            // Client probably disconnected
        }
    }
}
