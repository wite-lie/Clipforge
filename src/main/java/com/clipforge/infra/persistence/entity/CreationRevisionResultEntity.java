package com.clipforge.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationRevisionResultEntity（创作修正结果实体）。
 *
 * <p>该实体映射 cf_creation_revision_result（创作修正结果表），用于保存一次修正动作的前后版本关系。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_creation_revision_result")
public class CreationRevisionResultEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** revisionId（修正编号）是对外展示和关联使用的修正记录编号。 */
    private String revisionId;

    /** runId（运行编号）用于关联一次 Agent（智能体）执行。 */
    private String runId;

    /** sourceVersionId（来源版本编号）表示修正前方案。 */
    private String sourceVersionId;

    /** targetVersionId（目标版本编号）表示修正后方案。 */
    private String targetVersionId;

    /** evaluationId（评估编号）表示触发本次修正的评估结果。 */
    private String evaluationId;

    /** revisionReason（修正原因）保存触发修正的主要原因。 */
    private String revisionReason;

    /** revisionSummary（修正摘要）保存本次修正做了什么。 */
    private String revisionSummary;

    /** modelCallRecordId（模型调用记录编号）用于关联修正模型调用记录。 */
    private String modelCallRecordId;

    /** createTime（创建时间）保存修正记录创建时间。 */
    private LocalDateTime createTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
