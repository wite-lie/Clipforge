package com.clipforge.prompt.budget;

import com.clipforge.prompt.token.PromptSectionType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TokenBudgetSectionBudget（词元预算片段明细）。
 *
 * <p>该对象表示某个提示词片段的估算 Token（词元）、局部限制和超预算情况。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBudgetSectionBudget {

    /** sectionType（片段类型）表示提示词片段来源。 */
    private PromptSectionType sectionType;

    /** sectionName（片段名称）表示人类可读名称。 */
    private String sectionName;

    /** estimatedTokens（估算词元）表示该片段估算占用。 */
    private Integer estimatedTokens;

    /** limitTokens（限制词元）表示该片段局部预算；为空表示没有限制。 */
    private Integer limitTokens;

    /** overLimit（是否超过限制）表示该片段是否超过局部预算。 */
    private Boolean overLimit;

    /** overLimitTokens（超出词元）表示超过局部预算的数量。 */
    private Integer overLimitTokens;

    /** status（状态）表示 OK（正常）、OVER_LIMIT（超过限制）或 NO_LIMIT（无限制）。 */
    private TokenBudgetSectionStatus status;

    /** includedInTotal（是否计入总数）表示该片段是否属于真实发送给模型的最终消息。 */
    private Boolean includedInTotal;

    /** ratio（占比）表示该片段相对真实总输入 Token（词元）的占比。 */
    private BigDecimal ratio;
}
