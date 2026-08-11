package com.clipforge.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationEvaluationResultView（创作评估结果视图）。
 *
 * <p>该对象用于在质量复盘接口中展示每个方案版本的评估结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationEvaluationResultView {

    /** evaluationId（评估编号）表示评估结果业务编号。 */
    private String evaluationId;

    /** versionId（版本编号）表示被评估的方案版本。 */
    private String versionId;

    /** evaluatorType（评估器类型）表示 RULE（规则）或 LLM_JUDGE（大模型裁判）。 */
    private String evaluatorType;

    /** score（综合评分）表示评估总分。 */
    private Integer score;

    /** dimensionScoresJson（维度评分 JSON）保存各维度评分详情。 */
    private String dimensionScoresJson;

    /** strengthsJson（优点 JSON）保存优点列表。 */
    private String strengthsJson;

    /** weaknessesJson（不足 JSON）保存不足列表。 */
    private String weaknessesJson;

    /** revisionRequired（是否需要修正）表示是否触发修正。 */
    private Boolean revisionRequired;

    /** revisionAdviceJson（修正建议 JSON）保存评估器给出的修正建议。 */
    private String revisionAdviceJson;

    /** passed（是否通过）表示该版本是否通过质量阈值。 */
    private Boolean passed;

    /** modelCallRecordId（模型调用记录编号）表示评估关联的模型调用。 */
    private String modelCallRecordId;

    /** createTime（创建时间）表示评估结果创建时间。 */
    private LocalDateTime createTime;
}
