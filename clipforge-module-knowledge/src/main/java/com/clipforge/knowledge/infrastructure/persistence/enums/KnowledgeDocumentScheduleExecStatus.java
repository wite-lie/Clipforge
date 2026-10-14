package com.clipforge.knowledge.infrastructure.persistence.enums;

/**
 * KnowledgeDocumentScheduleExecStatus（知识文档定时执行状态）。
 *
 * <p>用于统一 cf_knowledge_document_schedule_exec（知识文档定时执行记录表）的 status（执行状态）字段取值。
 */
public enum KnowledgeDocumentScheduleExecStatus {

    /** RUNNING（执行中）表示当前同步流程正在运行。 */
    RUNNING,

    /** SUCCESS（成功）表示本次同步已完成。 */
    SUCCESS,

    /** FAILED（失败）表示本次同步失败。 */
    FAILED,

    /** SKIPPED（已跳过）表示本次同步因为内容未变化或租约冲突而被跳过。 */
    SKIPPED;

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库状态值
     */
    public String value() {
        return name();
    }
}
