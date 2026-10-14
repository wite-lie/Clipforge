package com.clipforge.prompt.token;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PromptTokenEstimateResult（提示词词元估算结果）。
 *
 * <p>该对象汇总一次模型调用前的 Prompt（提示词）长度估算结果，用于观测成本、上下文风险和后续裁剪策略。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTokenEstimateResult {

    /** totalEstimatedTokens（总估算词元数）表示真实发送给模型的片段合计估算 Token（词元）。 */
    private Integer totalEstimatedTokens;

    /** totalCharacters（总字符数）表示真实发送给模型的片段合计字符数。 */
    private Integer totalCharacters;

    /** riskLevel（风险等级）表示当前 Prompt（提示词）长度风险。 */
    private PromptTokenRiskLevel riskLevel;

    /** warningThreshold（警戒阈值）表示进入 MEDIUM（中）风险的估算 Token（词元）阈值。 */
    private Integer warningThreshold;

    /** highRiskThreshold（高风险阈值）表示进入 HIGH（高）风险的估算 Token（词元）阈值。 */
    private Integer highRiskThreshold;

    /** largestSectionType（最大片段类型）表示计入总数片段中 Token（词元）最多的片段类型。 */
    private PromptSectionType largestSectionType;

    /** largestSectionName（最大片段名称）表示计入总数片段中 Token（词元）最多的片段名称。 */
    private String largestSectionName;

    /** recommendedTrimSectionTypes（建议裁剪片段类型）表示后续优化项 7/8 可优先裁剪的诊断片段。 */
    private List<PromptSectionType> recommendedTrimSectionTypes;

    /** sections（片段估算列表）表示每个提示词片段的估算明细。 */
    private List<PromptTokenSectionEstimate> sections;
}
