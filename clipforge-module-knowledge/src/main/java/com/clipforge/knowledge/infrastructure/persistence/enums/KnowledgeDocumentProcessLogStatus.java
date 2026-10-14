package com.clipforge.knowledge.infrastructure.persistence.enums;

/**
 * KnowledgeDocumentProcessLogStatus（知识文档处理日志状态）。
 *
 * <p>该枚举用于统一 cf_knowledge_document_process_log（知识文档处理日志表）的 status（处理状态）字段取值。
 */
public enum KnowledgeDocumentProcessLogStatus {

    /** RUNNING（运行中）表示文档处理链路正在执行。 */
    RUNNING,

    /** SUCCESS（成功）表示文档处理链路已经完成。 */
    SUCCESS,

    /** FAILED（失败）表示文档处理链路失败。 */
    FAILED,

    /** SKIPPED（已跳过）表示文档无需重新处理或被调度策略跳过。 */
    SKIPPED;

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库处理日志状态值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
