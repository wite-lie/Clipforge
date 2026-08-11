package com.clipforge.prompt.token;

/**
 * PromptTokenRiskLevel（提示词词元风险等级）。
 *
 * <p>该枚举用于表达一次 Prompt（提示词）调用前的上下文长度风险。
 */
public enum PromptTokenRiskLevel {

    /** LOW（低）表示当前估算长度较安全。 */
    LOW,

    /** MEDIUM（中）表示当前估算长度需要关注。 */
    MEDIUM,

    /** HIGH（高）表示当前估算长度已经存在明显上下文或成本风险。 */
    HIGH;

    /**
     * value（字符串值）用于日志和接口展示。
     *
     * @return 风险等级字符串
     */
    public String value() {
        // 当前直接使用枚举名称作为稳定字符串。
        return name();
    }
}
