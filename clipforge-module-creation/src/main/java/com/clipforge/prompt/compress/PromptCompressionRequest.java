package com.clipforge.prompt.compress;

import com.clipforge.creation.model.AgentContext;
import com.clipforge.prompt.budget.TokenBudgetDecision;
import com.clipforge.prompt.token.PromptTokenEstimateResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PromptCompressionRequest（提示词压缩请求）。
 *
 * <p>该对象把压缩策略需要的上下文、Token（词元）估算和预算决策集中传递。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptCompressionRequest {

    /** context（智能体上下文）表示当前 Agent（智能体）运行上下文。 */
    private AgentContext context;

    /** estimateResult（估算结果）表示优化项 6 生成的 Token（词元）估算。 */
    private PromptTokenEstimateResult estimateResult;

    /** budgetDecision（预算决策）表示优化项 7 生成的预算判断。 */
    private TokenBudgetDecision budgetDecision;
}
