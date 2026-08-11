package com.clipforge.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationRevisionResultView（创作修正结果视图）。
 *
 * <p>该对象用于在质量复盘接口中展示修正前后版本关系。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationRevisionResultView {

    /** revisionId（修正编号）表示修正记录业务编号。 */
    private String revisionId;

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

    /** modelCallRecordId（模型调用记录编号）表示修正关联的模型调用。 */
    private String modelCallRecordId;

    /** createTime（创建时间）表示修正记录创建时间。 */
    private LocalDateTime createTime;
}
