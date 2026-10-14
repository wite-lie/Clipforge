package com.clipforge.creation.evaluation;

import com.clipforge.creation.model.AgentContext;
import com.clipforge.contract.creation.CreationPlanResult;

/**
 * CreationPlanEvaluator（创作方案评估器）。
 *
 * <p>该接口负责对 CreationPlanResult（创作方案结果）进行评分和补充改进建议。
 */
public interface CreationPlanEvaluator {

    /**
     * evaluateStructured（结构化评估）用于生成多维评估结果。
     *
     * @param context 智能体上下文
     * @param result 创作方案结果
     * @return 创作方案评估结果
     */
    CreationPlanEvaluationResult evaluateStructured(AgentContext context, CreationPlanResult result);

    /**
     * evaluate（评估）用于对创作方案进行质量评估。
     *
     * @param context 智能体上下文
     * @param result 创作方案结果
     * @return 评估后的创作方案结果
     */
    CreationPlanResult evaluate(AgentContext context, CreationPlanResult result);
}
