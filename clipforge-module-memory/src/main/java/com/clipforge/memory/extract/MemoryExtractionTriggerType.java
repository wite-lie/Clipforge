package com.clipforge.memory.extract;

/**
 * MemoryExtractionTriggerType（记忆抽取触发类型）。
 *
 * <p>该枚举用于区分一次记忆写入是在哪种高价值场景下触发的，方便控制是否启用 LLM（大语言模型）抽取。
 */
public enum MemoryExtractionTriggerType {

    /** CREATION_SUCCESS（创作成功）表示一次完整创作完成后的沉淀写入。 */
    CREATION_SUCCESS,

    /** PROJECT_INIT（项目初始化）表示项目刚建立或首次进入项目时的记忆整理。 */
    PROJECT_INIT,

    /** CONVERSATION_END（会话结束）表示用户结束本轮对话后的记忆收尾。 */
    CONVERSATION_END,

    /** MANUAL_CORRECTION（人工纠正）表示用户或管理员显式修正记忆时触发。 */
    MANUAL_CORRECTION;

    /**
     * value（数据库值）用于返回当前枚举的持久化字符串。
     *
     * @return 数据库存储值
     */
    public String value() {
        return name();
    }
}
