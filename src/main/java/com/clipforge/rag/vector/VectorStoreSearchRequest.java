package com.clipforge.rag.vector;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VectorStoreSearchRequest（向量存储检索请求）。
 *
 * <p>该对象用于承载 pgvector（PostgreSQL 向量扩展）TopK（前 K 个）相似度检索参数。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorStoreSearchRequest {

    /** knowledgeBaseId（知识库编号）用于限制检索范围；全库检索时可以为空。 */
    private String knowledgeBaseId;

    /** searchAllKnowledgeBases（检索全部知识库）表示不按 knowledgeBaseId（知识库编号）过滤。 */
    private Boolean searchAllKnowledgeBases;

    /** queryVector（查询向量）表示查询文本的 Embedding（嵌入）结果。 */
    private List<Float> queryVector;

    /** topK（前 K 个）表示最多返回多少条结果。 */
    private Integer topK;
}
