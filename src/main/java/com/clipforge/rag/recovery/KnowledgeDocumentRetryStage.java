package com.clipforge.rag.recovery;

/**
 * KnowledgeDocumentRetryStage（知识文档重试阶段）。
 *
 * <p>该枚举用于描述人工恢复时希望从哪个阶段重新处理知识文档。
 */
public enum KnowledgeDocumentRetryStage {

    /** FULL_PROCESS（完整处理）表示重新执行解析、切片、向量化和向量入库。 */
    FULL_PROCESS,

    /** PARSE_CHUNK（解析切片）表示从解析切片阶段开始，后续继续向量化。 */
    PARSE_CHUNK,

    /** EMBEDDING（向量化）表示复用已有切片，只重新调用 Embedding（嵌入）模型。 */
    EMBEDDING,

    /** PERSIST_VECTOR（向量入库）表示向量化与入库合并恢复，当前实现会重新执行 Embedding（嵌入）和向量入库。 */
    PERSIST_VECTOR;

    /**
     * value（文本值）用于写入消息或接口响应。
     *
     * @return 重试阶段文本
     */
    public String value() {
        return name();
    }
}
