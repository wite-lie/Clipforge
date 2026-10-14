package com.clipforge.creation.model;

import com.clipforge.contract.creation.CreationOutputType;
import com.clipforge.contract.creation.CreationPlanResult;
import com.clipforge.observability.trace.TraceEvent;
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

    /** outputType（产物类型）表示本次运行最终产出的内容形态。 */
    private CreationOutputType outputType = CreationOutputType.VIDEO_PLAN;

    /** outputPayload（通用产物内容）表示最终保存到 cf_creation_output 的真实结构化产物。 */
    private Object outputPayload;

    /**
     * setOutputType（设置产物类型）用于把应用层请求中的 outputType（产物类型）写入运行结果。
     *
     * @param outputType 产物类型
     */
    public void setOutputType(CreationOutputType outputType) {
        this.outputType = outputType == null ? CreationOutputType.VIDEO_PLAN : outputType;
    }

    /**
     * setOutputPayload（设置通用产物内容）用于保存短视频或图文的真实结构化结果。
     *
     * @param outputPayload 通用产物内容
     */
    public void setOutputPayload(Object outputPayload) {
        this.outputPayload = outputPayload;
    }
}
