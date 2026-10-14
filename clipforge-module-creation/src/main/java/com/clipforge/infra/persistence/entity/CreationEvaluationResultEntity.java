package com.clipforge.memory.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clipforge.memory.infrastructure.persistence.typehandler.JsonbStringTypeHandler;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationEvaluationResultEntity（创作评估结果实体）。
 *
 * <p>该实体映射 cf_creation_evaluation_result（创作评估结果表），用于保存规则评估或 LLM-as-Judge（大模型裁判）评估结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "cf_creation_evaluation_result", autoResultMap = true)
public class CreationEvaluationResultEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** evaluationId（评估编号）是对外展示和关联使用的评估结果编号。 */
    private String evaluationId;

    /** runId（运行编号）用于关联一次 Agent（智能体）执行。 */
    private String runId;

    /** versionId（版本编号）表示被评估的创作方案版本。 */
    private String versionId;

    /** evaluatorType（评估器类型）表示 rule（规则）或 llm（大模型）等来源。 */
    private String evaluatorType;

    /** score（综合评分）保存评估总分。 */
    private Integer score;

    /** dimensionScoresJson（维度评分 JSON）保存各评分维度详情。 */
    @TableField(value = "dimension_scores_json", typeHandler = JsonbStringTypeHandler.class)
    private String dimensionScoresJson;

    /** strengthsJson（优点 JSON）保存方案优点列表。 */
    @TableField(value = "strengths_json", typeHandler = JsonbStringTypeHandler.class)
    private String strengthsJson;

    /** weaknessesJson（不足 JSON）保存方案问题列表。 */
    @TableField(value = "weaknesses_json", typeHandler = JsonbStringTypeHandler.class)
    private String weaknessesJson;

    /** revisionRequired（是否需要修正）表示评估结果是否要求进入 Reflection（反思修正）。 */
    private Boolean revisionRequired;

    /** revisionAdviceJson（修正建议 JSON）保存评估器给出的改进建议。 */
    @TableField(value = "revision_advice_json", typeHandler = JsonbStringTypeHandler.class)
    private String revisionAdviceJson;

    /** passed（是否通过）表示该版本是否通过质量阈值。 */
    private Boolean passed;

    /** modelCallRecordId（模型调用记录编号）用于关联评估模型调用记录。 */
    private String modelCallRecordId;

    /** createTime（创建时间）保存评估结果创建时间。 */
    private LocalDateTime createTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
