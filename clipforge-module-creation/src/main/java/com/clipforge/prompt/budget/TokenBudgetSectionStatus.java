package com.clipforge.prompt.budget;

/**
 * TokenBudgetSectionStatus（词元预算片段状态）。
 *
 * <p>该枚举用于描述某个 PromptSection（提示词片段）是否超过自己的局部 Token（词元）预算。
 */
public enum TokenBudgetSectionStatus {

    /** OK（正常）表示片段没有超过局部预算。 */
    OK,

    /** OVER_LIMIT（超过限制）表示片段超过局部预算。 */
    OVER_LIMIT,

    /** NO_LIMIT（无限制）表示当前片段没有配置局部预算。 */
    NO_LIMIT;

    /**
     * value（字符串值）用于日志和测试输出。
     *
     * @return 状态字符串
     */
    public String value() {
        // 当前直接使用枚举名称作为稳定字符串。
        return name();
    }
}
