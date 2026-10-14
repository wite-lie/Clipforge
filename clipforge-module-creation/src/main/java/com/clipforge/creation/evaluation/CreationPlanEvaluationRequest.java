package com.clipforge.creation.evaluation;

import com.clipforge.creation.model.AgentContext;
import com.clipforge.contract.creation.CreationPlanResult;
import com.clipforge.creation.knowledge.CreativeKnowledge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationPlanEvaluationRequest（创作方案评估请求）。
 *
 * <p>该对象把评估所需的 AgentContext（智能体上下文）、CreationPlanResult（创作方案结果）和 CreativeKnowledge（创作知识）组合起来，后续 LLM-as-Judge（大模型裁判）评估器可以复用。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationPlanEvaluationRequest {

    /** context（智能体上下文）保存本次运行编号、目标平台和用户目标。 */
    private AgentContext context;

    /** planResult（创作方案结果）表示待评估的结构化方案。 */
    private CreationPlanResult planResult;

    /** creativeKnowledge（创作知识）表示知识库检索到的平台规则、脚本结构和评估提示。 */
    private CreativeKnowledge creativeKnowledge;
}
