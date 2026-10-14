package com.clipforge.rag.retrieval;

import com.clipforge.knowledge.retrieval.KnowledgeRetrievalProperties;
import com.clipforge.knowledge.retrieval.KeywordTokenizer;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalRequest;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalResult;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalSource;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievedChunk;
import com.clipforge.knowledge.retrieval.KnowledgeRetriever;
import com.clipforge.knowledge.retrieval.RrfHybridRanker;

import com.clipforge.knowledge.application.embedding.KnowledgeDocumentEmbeddingApplicationService;
import com.clipforge.knowledge.vector.VectorStoreSearchResult;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * VectorKnowledgeRetriever（向量知识检索器）。
 *
 * <p>该组件负责把 KnowledgeRetriever（知识检索器）传入的请求转交给 pgvector（PostgreSQL 向量扩展）检索能力，并把向量检索结果转换成统一的知识切片对象。
 */
@Component
@RequiredArgsConstructor
public class VectorKnowledgeRetriever {

    /** embeddingApplicationService（知识文档向量化应用服务）用于执行查询向量化和 pgvector（PostgreSQL 向量扩展）检索。 */
    private final KnowledgeDocumentEmbeddingApplicationService embeddingApplicationService;

    /**
     * retrieveCandidates（检索候选知识）用于从一个或多个知识库中召回向量候选切片。
     *
     * @param request 知识检索请求
     * @param topK 每个知识库最多召回的候选数量
     * @return 向量候选切片列表
     */
    public List<KnowledgeRetrievedChunk> retrieveCandidates(KnowledgeRetrievalRequest request, int topK) {
        // 如果请求启用全库检索，则不限制知识库编号，只执行一次全库向量召回。
        if (Boolean.TRUE.equals(request.getSearchAllKnowledgeBases())) {
            // 执行全库向量检索。
            List<VectorStoreSearchResult> results = embeddingApplicationService.searchByText(null, request.getQuery(), topK);
            // 转换为统一知识切片。
            return results == null ? List.of() : results.stream().map(this::toRetrievedChunk).toList();
        }
        // 如果请求没有传入知识库编号，则返回空列表，统一入口会负责静态降级。
        if (request.getKnowledgeBaseIds() == null || request.getKnowledgeBaseIds().isEmpty()) {
            // 返回空候选。
            return List.of();
        }
        // 创建候选切片列表。
        List<KnowledgeRetrievedChunk> chunks = new ArrayList<>();
        // 遍历知识库编号并执行向量检索。
        for (String knowledgeBaseId : request.getKnowledgeBaseIds()) {
            // 跳过空白知识库编号。
            if (knowledgeBaseId == null || knowledgeBaseId.isBlank()) {
                // 继续处理下一个知识库。
                continue;
            }
            // 对当前知识库执行一次检索。
            List<VectorStoreSearchResult> results = embeddingApplicationService.searchByText(knowledgeBaseId, request.getQuery(), topK);
            // 如果底层返回空结果，则跳过当前知识库。
            if (results == null || results.isEmpty()) {
                // 继续处理下一个知识库。
                continue;
            }
            // 把向量结果逐条转换成统一知识切片。
            for (VectorStoreSearchResult result : results) {
                // 追加转换后的知识切片。
                chunks.add(toRetrievedChunk(result));
            }
        }
        // 返回候选切片列表。
        return chunks;
    }

    /**
     * toRetrievedChunk（转换检索切片）用于把向量检索结果转换为统一结果对象。
     *
     * @param result 向量检索结果
     * @return 统一检索切片
     */
    private KnowledgeRetrievedChunk toRetrievedChunk(VectorStoreSearchResult result) {
        // 使用 Builder（构建器）创建统一检索切片。
        return KnowledgeRetrievedChunk.builder()
                // 设置来源为 pgvector（PostgreSQL 向量扩展）。
                .source(KnowledgeRetrievalSource.PGVECTOR)
                // 设置知识库编号。
                .knowledgeBaseId(result.getKnowledgeBaseId())
                // 设置文档编号。
                .documentId(result.getDocumentId())
                // 设置切片编号。
                .chunkId(result.getChunkId())
                // 设置切片序号。
                .chunkIndex(result.getChunkIndex())
                // 设置标题路径。
                .titlePath(result.getTitlePath())
                // 设置正文。
                .content(result.getContent())
                // 设置分数。
                .score(result.getScore())
                // 设置向量原始分数，便于后续 RRF（倒数排名融合）和 Trace（执行链路追踪）复盘。
                .vectorScore(result.getScore())
                // 设置向量距离。
                .distance(result.getDistance())
                // 设置元数据。
                .metadataJson(result.getMetadataJson())
                // 构建对象。
                .build();
    }
}
