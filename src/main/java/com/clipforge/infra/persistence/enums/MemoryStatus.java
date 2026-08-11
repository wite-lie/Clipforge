package com.clipforge.infra.persistence.enums;

/**
 * MemoryStatus（记忆状态）。
 *
 * <p>该枚举用于统一 Memory（记忆）相关表的 status（状态）字段取值。
 */
public enum MemoryStatus {

    /** ACTIVE（启用）表示记忆可以参与读取和 Prompt（提示词）注入。 */
    ACTIVE,

    /** DISABLED（停用）表示记忆被人工或系统停用，不再参与读取。 */
    DISABLED,

    /** EXPIRED（过期）表示记忆已经不再适用于当前用户或项目。 */
    EXPIRED;

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
