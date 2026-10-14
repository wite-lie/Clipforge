package com.clipforge.prompt.compress;

/**
 * PromptCompressionAction（提示词压缩动作）。
 *
 * <p>该枚举用于描述某个 Prompt（提示词）片段在压缩策略中的处理方式。
 */
public enum PromptCompressionAction {

    /** KEEP（保留）表示片段未被裁剪。 */
    KEEP,

    /** TRUNCATE_TAIL（截断尾部）表示保留片段开头并截断尾部。 */
    TRUNCATE_TAIL,

    /** DROP（丢弃）表示丢弃该片段。 */
    DROP,

    /** SKIP_EMPTY（跳过空片段）表示片段为空，无需处理。 */
    SKIP_EMPTY;

    /**
     * value（字符串值）用于日志和测试输出。
     *
     * @return 动作字符串
     */
    public String value() {
        // 返回枚举名称。
        return name();
    }
}
