package com.clipforge.creation.model;

import com.clipforge.contract.creation.CreationPlanResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CreationWorkflowResult（创作工作流结果）。
 *
 * <p>该对象用于把 Workflow（工作流）内部生成的 AgentPlan（智能体计划）和 CreationPlanResult（创作方案结果）一起返回给 Agent Runtime（智能体运行时）。
 */
@Getter
@AllArgsConstructor
public class CreationWorkflowResult {

    /** plan（智能体计划）表示本次工作流中 Planner（规划器）产出的计划。 */
    private final AgentPlan plan;

    /** result（创作方案结果）表示本次工作流最终产出的方案。 */
    private final CreationPlanResult result;

    /** outputPayload（通用产物内容）表示最终保存到 cf_creation_output 的真实结构化产物。 */
    private final Object outputPayload;

    public CreationWorkflowResult(AgentPlan plan, CreationPlanResult result) {
        this(plan, result, result);
    }
}
