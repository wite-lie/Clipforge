package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.AgentTraceEventEntity;
import com.clipforge.trace.TraceEvent;
import java.util.List;

/**
 * AgentTraceEventRepository（智能体执行链路事件仓储）。
 *
 * <p>该接口负责保存和查询 Agent（智能体）运行过程中的 Trace（执行链路追踪）事件。
 */
public interface AgentTraceEventRepository {

    /**
     * saveEvents（保存事件列表）用于批量保存一次运行产生的执行链路事件。
     *
     * @param traceEvents 执行链路事件列表
     * @return 已保存的执行链路事件实体列表
     */
    List<AgentTraceEventEntity> saveEvents(List<TraceEvent> traceEvents);

    /**
     * listByRunId（按运行编号查询列表）用于按发生时间升序查询执行链路事件。
     *
     * @param runId Agent（智能体）运行编号
     * @return 执行链路事件实体列表
     */
    List<AgentTraceEventEntity> listByRunId(String runId);
}
