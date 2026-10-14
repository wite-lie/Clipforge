package com.clipforge.prompt.budget;

import com.clipforge.prompt.token.PromptTokenRiskLevel;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TokenBudgetDecision（词元预算决策）。
 *
 * <p>该对象保存一次模型调用前的预算判断结果，只用于观测和后续裁剪依据，本阶段不阻断模型调用。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBudgetDecision {

    /** enabled（是否启用）表示当前是否启用预算策略。 */
    private Boolean enabled;

    /** purpose（调用用途）表示本次模型调用目的。 */
    private String purpose;

    /** profileName（预算档案名称）表示命中的预算档案。 */
    private String profileName;

    /** modelContextWindow（模型上下文窗口）表示总上下文容量。 */
    private Integer modelContextWindow;

    /** reservedOutputTokens（预留输出词元）表示为模型输出预留的 Token（词元）。 */
    private Integer reservedOutputTokens;

    /** reservedSafetyTokens（安全余量词元）表示为估算误差预留的 Token（词元）。 */
    private Integer reservedSafetyTokens;

    /** inputBudgetTokens（输入预算词元）表示本次 Prompt（提示词）可用输入预算。 */
    private Integer inputBudgetTokens;

    /** estimatedInputTokens（估算输入词元）表示优化项 6 得到的真实输入估算值。 */
    private Integer estimatedInputTokens;

    /** overBudget（是否超总预算）表示估算输入是否超过输入预算。 */
    private Boolean overBudget;

    /** overBudgetTokens（超出预算词元）表示超过总输入预算的 Token（词元）数量。 */
    private Integer overBudgetTokens;

    /** riskLevel（风险等级）表示预算策略综合判断后的风险等级。 */
    private PromptTokenRiskLevel riskLevel;

    /** sectionBudgets（片段预算明细）表示各片段的局部预算判断。 */
    private List<TokenBudgetSectionBudget> sectionBudgets;

    /** violatedSections（超预算片段）表示超过局部预算的片段列表。 */
    private List<TokenBudgetSectionBudget> violatedSections;

    /** recommendedActions（建议动作）表示后续优化项 8 可参考的裁剪或压缩建议。 */
    private List<String> recommendedActions;
}
