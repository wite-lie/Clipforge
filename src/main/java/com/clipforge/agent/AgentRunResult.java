package com.clipforge.agent;

import com.clipforge.trace.TraceEvent;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AgentRunResult（智能体运行结果）。
 *
 * <p>该对象汇总一次 Agent（智能体）运行后的计划、结果和执行链路。
 */
@Getter
@RequiredArgsConstructor
public class AgentRunResult {

    /** runId（运行编号）用于唯一标识一次智能体执行。 */
    private final String runId;

    /** plan（智能体计划）表示本次执行采用的步骤安排。 */
    private final AgentPlan plan;

    /** result（创作方案结果）表示本次执行生成的最终方案。 */
    private final CreationPlanResult result;

    /** traceEvents（执行链路事件）表示本次执行过程中的关键事件。 */
    private final List<TraceEvent> traceEvents;
}
