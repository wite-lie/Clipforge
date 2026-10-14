package com.clipforge.quality.evaluation;

/**
 * CreationEvaluationType（创作评估类型）。
 *
 * <p>该枚举用于统一质量复盘中常见评估器类型。
 */
public enum CreationEvaluationType {

    /** RULE（规则）表示由规则评估器产生的评估结果。 */
    RULE,

    /** LLM_JUDGE（大模型裁判）表示由 LLM-as-Judge（大模型裁判）产生的评估结果。 */
    LLM_JUDGE,

    /** UNKNOWN（未知）表示旧数据或第三方评估器未明确类型。 */
    UNKNOWN
}
