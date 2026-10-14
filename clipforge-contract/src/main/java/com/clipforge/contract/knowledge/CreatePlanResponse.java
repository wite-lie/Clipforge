package com.clipforge.contract.knowledge;

import com.clipforge.contract.creation.CreationOutputType;
import com.clipforge.contract.creation.CreationOutputView;
import com.clipforge.contract.creation.CreationPlanResult;
import com.clipforge.observability.trace.TraceEvent;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreatePlanResponse（创建方案响应）。
 *
 * <p>该对象把 Agent（智能体）运行编号、计划编号、创作结果和执行链路一起返回。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanResponse {

    /** runId（运行编号）用于唯一标识一次 Agent（智能体）执行。 */
    private String runId;

    /** projectId（项目编号）表示本次运行所属项目，未传入时可以为空。 */
    private String projectId;

    /** conversationId（会话编号）表示本次运行所属会话，未传入时可以为空。 */
    private String conversationId;

    /** planId（计划编号）用于唯一标识本次生成的 AgentPlan（智能体计划）。 */
    private String planId;

    /** outputType（产物类型）表示本次生成的是短视频方案还是图文方案。 */
    private CreationOutputType outputType;

    /** result（创作方案结果）承载最终生成的结构化创作方案。 */
    private CreationPlanResult result;

    /** output（通用创作产物）用于承载多产物类型的统一结果结构。 */
    private CreationOutputView output;

    /** traceEvents（执行链路事件列表）记录本次执行每个关键步骤。 */
    private List<TraceEvent> traceEvents;
}
