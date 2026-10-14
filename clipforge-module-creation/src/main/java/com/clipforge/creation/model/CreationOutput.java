package com.clipforge.creation.model;

import com.clipforge.contract.creation.CreationOutputType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationOutput（创作产物）。
 *
 * <p>该对象是创作模块内部的通用产物模型，用于承载短视频方案、图文方案和后续更多产物类型。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationOutput {

    /** outputId（产物编号）用于唯一标识一份创作产物。 */
    private String outputId;

    /** runId（运行编号）用于关联一次 Agent（智能体）运行。 */
    private String runId;

    /** taskId（任务编号）用于关联创作任务。 */
    private String taskId;

    /** outputType（产物类型）表示本次产物的内容形态。 */
    private CreationOutputType outputType;

    /** schemaVersion（结构版本）用于兼容后续 JSON 结构升级。 */
    private String schemaVersion;

    /** payloadJson（产物内容 JSON）保存完整结构化产物。 */
    private String payloadJson;

    /** createTime（创建时间）表示产物创建时间。 */
    private LocalDateTime createTime;
}
