package com.clipforge.infra.persistence.enums;

/**
 * KnowledgeDocumentStepStatus（知识文档步骤状态）。
 *
 * <p>该枚举用于统一 parseStatus（解析状态）、chunkStatus（切片状态）和 embeddingStatus（嵌入状态）这些阶段字段取值。
 */
public enum KnowledgeDocumentStepStatus {

    /** PENDING（待处理）表示该步骤还没有开始。 */
    PENDING,

    /** PROCESSING（处理中）表示该步骤正在执行。 */
    PROCESSING,

    /** SUCCESS（成功）表示该步骤已经完成。 */
    SUCCESS,

    /** FAILED（失败）表示该步骤执行失败。 */
    FAILED;

    /**
     * value（数据库值）用于返回当前枚举写入数据库的字符串。
     *
     * @return 数据库步骤状态值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
