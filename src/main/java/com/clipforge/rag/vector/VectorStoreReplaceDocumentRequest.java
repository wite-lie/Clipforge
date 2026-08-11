package com.clipforge.rag.vector;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VectorStoreReplaceDocumentRequest（向量存储替换文档请求）。
 *
 * <p>该对象用于承载“按文档编号替换全部切片向量”的短事务写入请求。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorStoreReplaceDocumentRequest {

    /** knowledgeBaseId（知识库编号）表示向量归属的知识库。 */
    private String knowledgeBaseId;

    /** documentId（知识文档编号）表示需要替换向量的文档。 */
    private String documentId;

    /** provider（供应商）表示生成向量的 Embedding（嵌入）模型供应商。 */
    private String provider;

    /** modelName（模型名称）表示生成向量的 Embedding（嵌入）模型名称。 */
    private String modelName;

    /** dimension（向量维度）表示每个向量的维度。 */
    private Integer dimension;

    /** distanceMetric（距离度量）表示 pgvector（PostgreSQL 向量扩展）检索使用的距离类型。 */
    private String distanceMetric;

    /** items（向量条目）表示需要写入的切片向量列表。 */
    private List<VectorStoreUpsertItem> items;
}
