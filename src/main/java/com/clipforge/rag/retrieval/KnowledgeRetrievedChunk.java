package com.clipforge.rag.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeRetrievedChunk（检索命中的知识切片）。
 *
 * <p>该对象统一表示向量知识库、关键词检索、混合检索命中的真实 chunk（切片）或静态降级生成的伪切片，便于后续 Prompt（提示词）拼接和 Trace（执行链路追踪）记录。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeRetrievedChunk {

    /** source（来源）表示该条结果来自向量检索、关键词检索、混合检索还是静态降级。 */
    private KnowledgeRetrievalSource source;

    /** knowledgeBaseId（知识库编号）表示命中切片所属知识库，静态降级时可以为空。 */
    private String knowledgeBaseId;

    /** documentId（知识文档编号）表示命中切片所属文档，静态降级时可以为空。 */
    private String documentId;

    /** chunkId（切片编号）表示命中的知识切片，静态降级时使用本地生成编号。 */
    private String chunkId;

    /** chunkIndex（切片序号）表示结果在当前来源中的顺序。 */
    private Integer chunkIndex;

    /** titlePath（标题路径）表示切片来源章节或静态知识类别。 */
    private String titlePath;

    /** content（正文）表示可注入 Prompt（提示词）的知识内容。 */
    private String content;

    /** score（分数）表示相似度或降级排序分数，越大越靠前。 */
    private Double score;

    /** vectorScore（向量分数）表示该切片在向量检索中的原始相似度分数。 */
    private Double vectorScore;

    /** keywordScore（关键词分数）表示该切片在 BM25（最佳匹配 25）关键词检索中的原始分数。 */
    private Double keywordScore;

    /** rrfScore（RRF 分数）表示 RRF（倒数排名融合）后的融合分数。 */
    private Double rrfScore;

    /** rerankScore（重排序分数）表示专用 Rerank Model（重排序模型）输出的最终相关性分数。 */
    private Double rerankScore;

    /** rerankIndex（重排序序号）表示该切片在 Rerank（重排序）结果中的最终位置。 */
    private Integer rerankIndex;

    /** distance（距离）表示向量检索距离，静态降级时为空。 */
    private Double distance;

    /** metadataJson（元数据 JSON）表示命中切片扩展元数据。 */
    private String metadataJson;
}
