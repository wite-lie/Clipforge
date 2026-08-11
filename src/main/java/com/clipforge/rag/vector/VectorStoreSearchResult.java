package com.clipforge.rag.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VectorStoreSearchResult（向量存储检索结果）。
 *
 * <p>该对象表示 pgvector（PostgreSQL 向量扩展）命中的一个知识切片。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorStoreSearchResult {

    /** knowledgeBaseId（知识库编号）表示命中切片所属知识库。 */
    private String knowledgeBaseId;

    /** documentId（知识文档编号）表示命中切片所属文档。 */
    private String documentId;

    /** chunkId（知识切片编号）表示命中的切片。 */
    private String chunkId;

    /** chunkIndex（切片序号）表示切片在原文中的顺序。 */
    private Integer chunkIndex;

    /** titlePath（标题路径）表示切片来源章节。 */
    private String titlePath;

    /** content（正文）表示命中的切片文本。 */
    private String content;

    /** metadataJson（元数据 JSON）表示切片扩展元数据。 */
    private String metadataJson;

    /** distance（距离）表示 pgvector（PostgreSQL 向量扩展）返回的余弦距离，越小越相关。 */
    private Double distance;

    /** score（分数）表示由距离转换得到的相似度分数，越大越相关。 */
    private Double score;
}
