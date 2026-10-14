package com.clipforge.contract.creation;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationOutputView（创作产物视图）。
 *
 * <p>该对象用于向前端返回通用创作产物信息。不同产物类型的结构化内容统一放在 payloadJson 中。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationOutputView {

    /** outputId（产物编号）用于唯一标识一份创作产物。 */
    private String outputId;

    /** runId（运行编号）用于关联一次 Agent（智能体）运行。 */
    private String runId;

    /** taskId（任务编号）用于关联创作任务。 */
    private String taskId;

    /** outputType（产物类型）表示短视频方案、图文方案等内容形态。 */
    private CreationOutputType outputType;

    /** schemaVersion（结构版本）用于后续平滑升级不同产物 JSON 结构。 */
    private String schemaVersion;

    /** payloadJson（产物内容 JSON）保存完整结构化产物。 */
    private String payloadJson;

    /** createTime（创建时间）表示产物保存时间。 */
    private LocalDateTime createTime;
}
