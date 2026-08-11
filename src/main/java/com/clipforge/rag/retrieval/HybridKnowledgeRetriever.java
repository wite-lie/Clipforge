package com.clipforge.rag.retrieval;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * HybridKnowledgeRetriever（混合知识检索器）。
 *
 * <p>该组件用于 Phase 7.10.5（阶段 7.10.5）串联 VectorKnowledgeRetriever（向量知识检索器）、KeywordKnowledgeRetriever（关键词知识检索器）和 RrfHybridRanker（RRF 混合排序器）。
 */
@Component
@RequiredArgsConstructor
public class HybridKnowledgeRetriever {

    /** vectorKnowledgeRetriever（向量知识检索器）负责 pgvector（PostgreSQL 向量扩展）候选召回。 */
    private final VectorKnowledgeRetriever vectorKnowledgeRetriever;

    /** keywordKnowledgeRetriever（关键词知识检索器）负责 BM25（最佳匹配 25）关键词候选召回。 */
    private final KeywordKnowledgeRetriever keywordKnowledgeRetriever;

    /** rrfHybridRanker（RRF 混合排序器）负责把两路候选融合排序。 */
    private final RrfHybridRanker rrfHybridRanker;

    /**
     * retrieveHybridCandidates（检索混合候选）用于返回 RRF（倒数排名融合）后的知识切片。
     *
     * @param request 知识检索请求
     * @param topK 最终返回数量
     * @return 混合检索候选列表
     */
    public List<KnowledgeRetrievedChunk> retrieveHybridCandidates(KnowledgeRetrievalRequest request, int topK) {
        // 召回向量候选。
        List<KnowledgeRetrievedChunk> vectorChunks = vectorKnowledgeRetriever.retrieveCandidates(request, topK);
        // 召回关键词候选。
        List<KnowledgeRetrievedChunk> keywordChunks = keywordKnowledgeRetriever.retrieveCandidates(request, topK);
        // 使用 RRF（倒数排名融合）融合两路候选。
        return rrfHybridRanker.rank(sortByScoreDesc(vectorChunks), sortByScoreDesc(keywordChunks), topK);
    }

    /**
     * retrieveVectorCandidates（检索向量候选）用于在关闭混合检索时保留原向量检索行为。
     *
     * @param request 知识检索请求
     * @param topK 最终返回数量
     * @return 向量候选列表
     */
    public List<KnowledgeRetrievedChunk> retrieveVectorCandidates(KnowledgeRetrievalRequest request, int topK) {
        // 召回向量候选并按分数倒序截断。
        return sortByScoreDesc(vectorKnowledgeRetriever.retrieveCandidates(request, topK)).stream()
                // 截断到 TopK（前 K 个）。
                .limit(Math.max(topK, 1))
                // 收集为列表。
                .toList();
    }

    /**
     * sortByScoreDesc（按分数倒序排序）用于保证 RRF（倒数排名融合）的 rank（排名）输入稳定。
     *
     * @param chunks 候选切片
     * @return 排序后的候选切片
     */
    private List<KnowledgeRetrievedChunk> sortByScoreDesc(List<KnowledgeRetrievedChunk> chunks) {
        // 如果候选为空，则返回空列表。
        if (chunks == null || chunks.isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 按 score（分数）倒序排序。
        return chunks.stream()
                // 分数越大越靠前。
                .sorted(Comparator.comparing(KnowledgeRetrievedChunk::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                // 收集为列表。
                .toList();
    }
}
