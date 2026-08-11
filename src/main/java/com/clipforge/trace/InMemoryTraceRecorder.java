package com.clipforge.trace;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * InMemoryTraceRecorder（内存执行链路记录器）。
 *
 * <p>用于在本地开发和测试阶段保存执行链路事件，方便排查模型调用、工具调用和任务流转过程。
 */
@Component
public class InMemoryTraceRecorder implements TraceRecorder {

    /**
     * 未知运行编号。
     *
     * <p>当调用方没有传入 runId 时，统一落到这个分组，避免空键导致并发容器报错。
     */
    private static final String UNKNOWN_RUN_ID = "unknown-run";

    /** 按 runId 保存事件列表。 */
    private final Map<String, List<TraceEvent>> eventStore = new ConcurrentHashMap<>();

    @Override
    public void record(String runId, String eventType, String message, String detail) {
        String normalizedRunId = normalizeRunId(runId);
        TraceEvent event = new TraceEvent(
                normalizedRunId,
                eventType,
                message,
                detail,
                OffsetDateTime.now());
        eventStore.computeIfAbsent(normalizedRunId, key -> Collections.synchronizedList(new ArrayList<>()))
                .add(event);
    }

    @Override
    public List<TraceEvent> findByRunId(String runId) {
        String normalizedRunId = normalizeRunId(runId);
        List<TraceEvent> events = eventStore.get(normalizedRunId);
        if (events == null) {
            return List.of();
        }
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    private String normalizeRunId(String runId) {
        if (runId == null || runId.isBlank()) {
            return UNKNOWN_RUN_ID;
        }
        return runId.trim();
    }
}
