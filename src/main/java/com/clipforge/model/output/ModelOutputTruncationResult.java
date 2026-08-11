package com.clipforge.model.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ModelOutputTruncationResult（模型输出截断识别结果）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelOutputTruncationResult {

    /** truncated（是否疑似截断）表示输出是否很可能没有返回完整。 */
    private boolean truncated;

    /** repairable（是否可修复）表示是否建议发起一次模型修复。 */
    private boolean repairable;

    /** issueType（问题类型）表示截断或结构问题的分类。 */
    private ModelOutputIssueType issueType;

    /** confidence（置信度）表示规则判断的可信程度，范围 0 到 1。 */
    private double confidence;

    /** reason（原因）用于 Trace（执行链路追踪）和错误提示。 */
    private String reason;

    /** responseLength（响应字符数）表示原始输出长度。 */
    private int responseLength;
}
