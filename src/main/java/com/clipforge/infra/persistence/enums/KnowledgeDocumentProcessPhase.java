package com.clipforge.infra.persistence.enums;

/**
 * KnowledgeDocumentProcessPhase（知识文档处理阶段）。
 *
 * <p>该枚举用于统一 cf_knowledge_document_process_log（知识文档处理日志表）的 phase（处理阶段）字段取值。
 */
public enum KnowledgeDocumentProcessPhase {

    /** PARSE_CHUNK（解析切片）表示正在解析文档并生成知识切片。 */
    PARSE_CHUNK,

    /** EMBEDDING（嵌入）表示正在调用 Embedding（嵌入）模型并写入向量。 */
    EMBEDDING,

    /** READY（可检索）表示完整处理已经成功完成。 */
    READY;

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库处理阶段值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
