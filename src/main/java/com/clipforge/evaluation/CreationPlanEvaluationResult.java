package com.clipforge.evaluation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationPlanEvaluationResult（创作方案评估结果）。
 *
 * <p>该对象承载多维 Evaluation（评估）结果，用于后续 Reflection（反思修正）判断和 Trace（执行链路追踪）复盘。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationPlanEvaluationResult {

    /** overallScore（综合评分）表示按多个维度汇总后的 0 到 100 分。 */
    private int overallScore;

    /** dimensionScores（维度评分列表）保存每个评估维度的分数和原因。 */
    private List<EvaluationDimensionScore> dimensionScores;

    /** strengths（优点列表）表示当前方案表现较好的地方。 */
    private List<String> strengths;

    /** weaknesses（不足列表）表示当前方案需要改进的地方。 */
    private List<String> weaknesses;

    /** revisionRequired（是否需要修正）表示该方案是否低于质量阈值或存在关键问题。 */
    private boolean revisionRequired;

    /** revisionAdvice（修正建议）表示后续修正模型或人工优化可以参考的动作。 */
    private List<String> revisionAdvice;

    /** evaluatorType（评估器类型）表示结果来自 rule（规则）还是 llm（大模型）。 */
    private String evaluatorType;
}
