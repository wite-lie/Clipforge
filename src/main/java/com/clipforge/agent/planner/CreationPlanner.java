package com.clipforge.agent.planner;

import com.clipforge.agent.AgentContext;
import com.clipforge.agent.AgentPlan;

/**
 * CreationPlanner（创作规划器）。
 *
 * <p>该接口负责根据 AgentContext（智能体上下文）生成 AgentPlan（智能体计划）。
 */
public interface CreationPlanner {

    /**
     * plan（规划）用于生成一次创作任务内部的执行计划。
     *
     * @param context 智能体上下文
     * @return 智能体计划
     */
    AgentPlan plan(AgentContext context);
}

