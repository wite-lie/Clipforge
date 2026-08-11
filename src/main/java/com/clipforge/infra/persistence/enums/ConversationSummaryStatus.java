package com.clipforge.infra.persistence.enums;

/**
 * ConversationSummaryStatus（会话摘要状态）。
 *
 * <p>该枚举用于统一 cf_conversation_summary（会话滚动摘要表）的 status（状态）字段取值。
 */
public enum ConversationSummaryStatus {

    /** ACTIVE（启用）表示摘要可以参与短期记忆上下文构建。 */
    ACTIVE,

    /** STALE（待刷新）表示摘要已经落后，需要在下一次压缩时重写。 */
    STALE,

    /** DISABLED（停用）表示摘要被人工或系统停用。 */
    DISABLED;

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库状态值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
