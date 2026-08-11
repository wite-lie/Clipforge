package com.clipforge.agent.generator;

import com.clipforge.agent.AgentContext;
import com.clipforge.agent.AgentPlan;
import com.clipforge.agent.CreationPlanResult;

/**
 * CreationPlanGenerator（创作方案生成器）。
 *
 * <p>该接口负责根据用户输入、创作知识和 AgentPlan（智能体计划）生成创作方案，后续会接入 LLM（大语言模型）。
 */
public interface CreationPlanGenerator {

    /**
     * generate（生成）用于生成一份创作方案。
     *
     * @param context 智能体上下文
     * @param plan 智能体计划
     * @return 创作方案结果
     */
    CreationPlanResult generate(AgentContext context, AgentPlan plan);
}

