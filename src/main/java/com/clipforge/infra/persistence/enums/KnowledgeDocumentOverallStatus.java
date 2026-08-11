package com.clipforge.infra.persistence.enums;

/**
 * KnowledgeDocumentOverallStatus（知识文档整体状态）。
 *
 * <p>该枚举用于统一 cf_knowledge_document（知识文档表）的 overall_status（整体状态）字段取值。
 */
public enum KnowledgeDocumentOverallStatus {

    /** PENDING（待处理）表示文档已上传但尚未进入处理链路。 */
    PENDING,

    /** PROCESSING（处理中）表示文档正在被解析、切片或向量化。 */
    PROCESSING,

    /** CHUNKED（已切片）表示文档已经完成解析和切片，但尚未完成向量化。 */
    CHUNKED,

    /** PENDING_REINDEX（待重新索引）表示切片被人工修改后，文档需要重新向量化。 */
    PENDING_REINDEX,

    /** READY（可检索）表示文档已经完成向量化，可以参与 RAG（检索增强生成）检索。 */
    READY,

    /** FAILED（失败）表示文档最近一次处理失败。 */
    FAILED;

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库整体状态值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
