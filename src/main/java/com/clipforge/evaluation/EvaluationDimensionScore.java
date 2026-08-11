package com.clipforge.evaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * EvaluationDimensionScore（评估维度分数）。
 *
 * <p>该对象表示 CreationPlanEvaluationResult（创作方案评估结果）中的一个具体评分维度。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDimensionScore {

    /** dimension（评估维度）表示当前分数对应的维度编码，例如 hookStrength（开头吸引力）。 */
    private String dimension;

    /** dimensionName（评估维度名称）表示给用户和测试日志阅读的中文名称。 */
    private String dimensionName;

    /** score（分数）表示当前维度的 0 到 100 分评分。 */
    private int score;

    /** reason（评分原因）表示为什么给出当前维度分数。 */
    private String reason;
}
