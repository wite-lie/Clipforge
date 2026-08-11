package com.clipforge.prompt.budget;

import com.clipforge.prompt.token.PromptTokenEstimateResult;

/**
 * TokenBudgetPolicy（词元预算策略）。
 *
 * <p>该接口用于根据模型调用用途和 PromptTokenEstimateResult（提示词词元估算结果）生成预算决策。
 */
public interface TokenBudgetPolicy {

    /**
     * decide（决策）用于生成一次模型调用前的 Token Budget（词元预算）判断。
     *
     * @param purpose 模型调用用途
     * @param estimateResult 提示词词元估算结果
     * @return 词元预算决策
     */
    TokenBudgetDecision decide(String purpose, PromptTokenEstimateResult estimateResult);
}
