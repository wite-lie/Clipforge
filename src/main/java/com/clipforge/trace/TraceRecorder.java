package com.clipforge.trace;

import java.util.List;

/**
 * TraceRecorder（执行链路记录器）。
 *
 * <p>该接口定义 Agent（智能体）执行事件的记录和查询能力。
 */
public interface TraceRecorder {

    /**
     * record（记录事件）用于写入一条执行链路事件。
     *
     * @param runId 运行编号
     * @param eventType 事件类型
     * @param message 事件消息
     * @param detail 事件详情
     */
    void record(String runId, String eventType, String message, String detail);

    /**
     * findByRunId（按运行编号查询）用于读取一次执行的全部事件。
     *
     * @param runId 运行编号
     * @return 执行链路事件列表
     */
    List<TraceEvent> findByRunId(String runId);
}

