package com.clipforge.rag.retrieval;

/**
 * KnowledgeRetrievalSource（知识检索来源）。
 *
 * <p>该枚举用于标识当前检索结果来自 pgvector（PostgreSQL 向量扩展）、BM25（最佳匹配 25）关键词检索、混合检索还是静态 Markdown（标记文档）降级知识。
 */
public enum KnowledgeRetrievalSource {

    /** PGVECTOR（向量检索）表示结果来自正式向量知识库。 */
    PGVECTOR,

    /** KEYWORD（关键词检索）表示结果来自 BM25（最佳匹配 25）等关键词检索能力。 */
    KEYWORD,

    /** HYBRID（混合检索）表示结果由向量检索和关键词检索融合得到。 */
    HYBRID,

    /** STATIC_FALLBACK（静态降级）表示结果来自本地 Markdown（标记文档）静态知识库。 */
    STATIC_FALLBACK;

    /**
     * value（取值）用于返回可写入日志或接口响应的字符串。
     *
     * @return 来源字符串
     */
    public String value() {
        // 当前直接返回枚举名称。
        return name();
    }
}
