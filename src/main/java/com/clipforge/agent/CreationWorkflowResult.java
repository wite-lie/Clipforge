package com.clipforge.agent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * CreationWorkflowResult（创作工作流结果）。
 *
 * <p>该对象用于把 Workflow（工作流）内部生成的 AgentPlan（智能体计划）和 CreationPlanResult（创作方案结果）一起返回给 Agent Runtime（智能体运行时）。
 */
@Getter
@RequiredArgsConstructor
public class CreationWorkflowResult {

    /** plan（智能体计划）表示本次工作流中 Planner（规划器）产出的计划。 */
    private final AgentPlan plan;

    /** result（创作方案结果）表示本次工作流最终产出的方案。 */
    private final CreationPlanResult result;
}
