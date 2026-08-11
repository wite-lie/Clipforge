package com.clipforge.evaluation;

import com.clipforge.agent.AgentContext;
import com.clipforge.agent.CreationPlanResult;

/**
 * CreationPlanRevisionGenerator（创作方案修正生成器）。
 *
 * <p>该接口负责根据评估结果对低质量 CreationPlanResult（创作方案结果）执行有限 Reflection（反思修正）。
 */
public interface CreationPlanRevisionGenerator {

    /**
     * revise（修正）用于根据评估结果生成修正后的创作方案。
     *
     * @param context 智能体上下文
     * @param originalResult 原始创作方案结果
     * @param evaluationResult 创作方案评估结果
     * @return 修正后的创作方案结果
     */
    CreationPlanResult revise(AgentContext context, CreationPlanResult originalResult, CreationPlanEvaluationResult evaluationResult);
}
