package com.clipforge.knowledge.infrastructure.persistence.enums;

/**
 * KnowledgeDocumentProcessTriggerType（知识文档处理触发类型）。
 *
 * <p>该枚举用于统一 cf_knowledge_document_process_log（知识文档处理日志表）的 trigger_type（触发类型）字段取值。
 */
public enum KnowledgeDocumentProcessTriggerType {

    /** MANUAL（手动触发）表示用户或测试接口直接触发文档处理。 */
    MANUAL,

    /** SCHEDULE（定时触发）表示后续 URL（统一资源定位符）定时同步触发处理。 */
    SCHEDULE,

    /** MQ_RETRY（消息重试）表示后续 RocketMQ（消息队列）重试触发处理。 */
    MQ_RETRY,

    /** SYSTEM_SEED（系统种子）表示系统初始化知识文件触发处理。 */
    SYSTEM_SEED;

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库触发类型值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
