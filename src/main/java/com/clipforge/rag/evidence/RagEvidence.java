package com.clipforge.rag.evidence;

import com.clipforge.rag.retrieval.KnowledgeRetrievalSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RagEvidence（RAG 证据）。
 *
 * <p>该对象表示最终准备注入 Prompt（提示词）的知识证据，保留来源、分数、排序和裁剪信息，方便 Trace（执行链路追踪）复盘。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagEvidence {

    /** evidenceIndex（证据序号）表示该证据在最终 Prompt（提示词）中的顺序，从 1 开始。 */
    private Integer evidenceIndex;

    /** knowledgeBaseId（知识库编号）表示证据来自哪个知识库。 */
    private String knowledgeBaseId;

    /** documentId（文档编号）表示证据来自哪个知识文档。 */
    private String documentId;

    /** chunkId（切片编号）表示证据对应的知识切片。 */
    private String chunkId;

    /** chunkIndex（切片序号）表示证据在原文档中的切片位置。 */
    private Integer chunkIndex;

    /** titlePath（标题路径）表示证据所在章节。 */
    private String titlePath;

    /** source（检索来源）表示证据来自向量检索、关键词检索、混合检索或静态降级。 */
    private KnowledgeRetrievalSource source;

    /** finalScore（最终分数）优先使用 Rerank（重排序）分数，否则使用融合分数或向量分数。 */
    private Double finalScore;

    /** vectorScore（向量分数）表示向量检索原始相似度。 */
    private Double vectorScore;

    /** keywordScore（关键词分数）表示 BM25（最佳匹配 25）关键词分数。 */
    private Double keywordScore;

    /** rrfScore（倒数排名融合分数）表示 RRF（倒数排名融合）后的分数。 */
    private Double rrfScore;

    /** rerankScore（重排序分数）表示专用 Rerank（重排序）模型输出的相关性分数。 */
    private Double rerankScore;

    /** content（证据正文）表示最终注入 Prompt（提示词）的内容。 */
    private String content;

    /** truncated（是否被截断）表示该证据是否因为预算保护发生了内容截断。 */
    private boolean truncated;

    /** filteredReason（过滤原因）为空表示该证据进入 Prompt（提示词），非空表示被过滤。 */
    private String filteredReason;
}
