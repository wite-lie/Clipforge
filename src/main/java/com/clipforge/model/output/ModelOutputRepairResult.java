package com.clipforge.model.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ModelOutputRepairResult（模型输出修复结果）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelOutputRepairResult {

    /** status（修复状态）表示最终修复或降级结果。 */
    private ModelOutputRepairStatus status;

    /** truncationResult（截断识别结果）表示修复前诊断信息。 */
    private ModelOutputTruncationResult truncationResult;

    /** originalContent（原始内容）表示第一次模型输出。 */
    private String originalContent;

    /** repairedContent（修复内容）表示二次修复输出。 */
    private String repairedContent;

    /** message（说明）用于 Trace（执行链路追踪）和接口提示。 */
    private String message;
}
