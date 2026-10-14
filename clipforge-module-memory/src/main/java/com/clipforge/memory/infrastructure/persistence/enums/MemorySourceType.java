package com.clipforge.memory.infrastructure.persistence.enums;

/**
 * MemorySourceType（记忆来源类型）。
 *
 * <p>该枚举用于描述一条 Memory（记忆）是来自用户明确表达、系统推断、历史高分结果、LLM 推断还是人工修正。
 */
public enum MemorySourceType {

    /** USER_EXPLICIT（用户明确表达）表示用户直接说出了偏好或约束。 */
    USER_EXPLICIT,

    /** SYSTEM_INFERRED（系统推断）表示系统根据上下文或历史结果推断得到。 */
    SYSTEM_INFERRED,

    /** HIGH_SCORE_RESULT（高分结果）表示从高分创作方案中沉淀出来。 */
    HIGH_SCORE_RESULT,

    /** LLM_INFERRED（LLM 推断）表示通过大模型补充抽取得到。 */
    LLM_INFERRED,

    /** MANUAL（人工维护）表示由人工管理接口写入或修正。 */
    MANUAL;

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库存储值
     */
    public String value() {
        return name();
    }
}
