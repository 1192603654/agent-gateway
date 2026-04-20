package com.agent.gateway.server.controller;

import com.agent.gateway.core.CollaborationListener;
import com.agent.gateway.server.service.GatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 网关核心控制器
 * 处理来自第三方系统或前端的意图请求，并以 SSE 流式返回进度。
 */
@Tag(name = "Gateway API", description = "网关对外统一调用接口，支持第三方系统集成。")
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
@Slf4j
public class GatewayController {
    private final GatewayService gatewayService;
    private final ObjectMapper objectMapper;
    // 使用 Java 21 虚拟线程池，支持高并发 SSE 连接
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Operation(summary = "提交意图请求 (SSE 流式)", description = "第三方系统通过此接口提交用户意图，网关将以服务器发送事件 (SSE) 的形式实时返回执行进度和最终结果。")
    @PostMapping(value = "/query", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter query(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        SseEmitter emitter = new SseEmitter(180_000L); // 3 分钟超时

        executor.execute(() -> {
            try {
                gatewayService.processStream(query, request, new CollaborationListener() {
                    @Override
                    public void onStepStart(String agentName, int step) {
                        sendEvent(emitter, "step_start", Map.of("agent", agentName, "step", step));
                    }

                    @Override
                    public void onStepChunk(String agentName, Object chunk) {
                        // chunk 可能是字符串，也可能是子 Agent 返回的原始 JSON 结构
                        sendEvent(emitter, "step_chunk", Map.of("agent", agentName, "chunk", chunk));
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
                    public void onMetadata(String key, Object value) {
                        sendEvent(emitter, "metadata", Map.of("key", key, "value", value));
                    }

                    @Override
                    public void onError(String message) {
                        sendEvent(emitter, "error", Map.of("message", message));
                        emitter.complete();
                    }
                });
            } catch (Exception e) {
                log.error("流式处理异常", e);
                sendEvent(emitter, "error", Map.of("message", e.getMessage()));
                emitter.complete();
            }
        });

        return emitter;
    }

    /**
     * 手动序列化并发送 SSE 事件，避免 Spring 异步消息转换异常
     */
    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name(name).data(json));
        } catch (Exception e) {
            log.warn("无法发送 SSE 事件: {}", name);
        }
    }
}
