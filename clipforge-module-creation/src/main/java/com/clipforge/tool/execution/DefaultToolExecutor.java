package com.clipforge.tool.execution;

import com.clipforge.tool.definition.ToolDefinition;
import com.clipforge.tool.registry.ToolRegistry;
import com.clipforge.observability.trace.TraceRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DefaultToolExecutor（默认工具执行器）。
 *
 * <p>该实现先提供同步工具调用的统一入口，记录工具开始、成功和失败事件。后续可以在这里继续接入超时、重试、降级和并发控制。
 */
@Component
@RequiredArgsConstructor
public class DefaultToolExecutor implements ToolExecutor {

    /** toolRegistry（工具注册表）用于读取工具定义和策略。 */
    private final ToolRegistry toolRegistry;

    /** traceRecorder（执行链路记录器）用于记录工具调用事件。 */
    private final TraceRecorder traceRecorder;

    @Override
    public <T> ToolInvocationResult<T> execute(ToolInvocationRequest<T> request) {
        validate(request);
        String toolName = request.getToolName();
        ToolDefinition definition = toolRegistry.findDefinition(toolName).orElse(null);
        long startedAt = System.currentTimeMillis();
        traceRecorder.record(request.getRunId(), "TOOL_INVOCATION_STARTED", "工具调用开始", buildStartDetail(request, definition));
        try {
            T output = request.getAction().execute();
            long latencyMs = System.currentTimeMillis() - startedAt;
            traceRecorder.record(request.getRunId(), "TOOL_INVOCATION_SUCCEEDED", "工具调用成功", "toolName：" + toolName + "，latencyMs：" + latencyMs);
            return ToolInvocationResult.<T>builder()
                    .toolName(toolName)
                    .status(ToolInvocationStatus.SUCCESS)
                    .output(output)
                    .latencyMs(latencyMs)
                    .build();
        } catch (RuntimeException exception) {
            long latencyMs = System.currentTimeMillis() - startedAt;
            traceRecorder.record(request.getRunId(), "TOOL_INVOCATION_FAILED", "工具调用失败", "toolName：" + toolName + "，latencyMs：" + latencyMs + "，error：" + exception.getMessage());
            throw new ToolInvocationException(toolName, "工具调用失败：" + toolName, exception);
        }
    }

    private void validate(ToolInvocationRequest<?> request) {
        if (request == null) {
            throw new ToolInvocationException(null, "工具调用请求不能为空", null);
        }
        if (request.getToolName() == null || request.getToolName().isBlank()) {
            throw new ToolInvocationException(null, "工具名称不能为空", null);
        }
        if (request.getAction() == null) {
            throw new ToolInvocationException(request.getToolName(), "工具动作不能为空", null);
        }
    }

    private String buildStartDetail(ToolInvocationRequest<?> request, ToolDefinition definition) {
        StringBuilder builder = new StringBuilder();
        builder.append("toolName：").append(request.getToolName());
        if (definition != null) {
            builder.append("，effectType：").append(definition.getEffectType());
            builder.append("，llmCallable：").append(definition.llmCallable());
        }
        if (request.getInputSummary() != null && !request.getInputSummary().isBlank()) {
            builder.append("，input：").append(request.getInputSummary());
        }
        return builder.toString();
    }
}
