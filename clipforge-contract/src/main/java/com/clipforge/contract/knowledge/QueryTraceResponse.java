package com.clipforge.contract.knowledge;

import com.clipforge.observability.trace.TraceEvent;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * QueryTraceResponse（查询执行链路响应）。
 *
 * <p>该对象用于返回已经持久化到数据库中的 Trace（执行链路追踪）事件列表。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryTraceResponse {

    /** runId（运行编号）用于唯一标识一次 Agent（智能体）执行。 */
    private String runId;

    /** traceEvents（执行链路事件列表）按事件发生时间升序排列。 */
    private List<TraceEvent> traceEvents;
}
