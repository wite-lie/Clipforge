package com.clipforge.creation.article;

import com.clipforge.contract.creation.ArticleCreationPlan;
import com.clipforge.creation.model.AgentContext;
import com.clipforge.creation.model.AgentPlan;

/**
 * ArticleCreationPlanGenerator（图文创作方案生成器）。
 */
public interface ArticleCreationPlanGenerator {

    /**
     * generate（生成）用于根据当前创作上下文生成图文方案。
     *
     * @param context 智能体上下文
     * @param plan 智能体计划
     * @return 图文创作方案
     */
    ArticleCreationPlan generate(AgentContext context, AgentPlan plan);
}
