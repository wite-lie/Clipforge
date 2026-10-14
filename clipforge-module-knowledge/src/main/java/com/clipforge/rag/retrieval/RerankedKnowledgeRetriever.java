package com.clipforge.rag.retrieval;

import com.clipforge.knowledge.retrieval.KnowledgeRetrievalProperties;
import com.clipforge.knowledge.retrieval.KeywordTokenizer;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalRequest;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalResult;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalSource;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievedChunk;
import com.clipforge.knowledge.retrieval.KnowledgeRetriever;
import com.clipforge.knowledge.retrieval.RrfHybridRanker;

import com.clipforge.ai.api.rerank.RerankCandidate;
import com.clipforge.ai.api.rerank.RerankModelClient;
import com.clipforge.ai.infrastructure.rerank.RerankModelProperties;
import com.clipforge.ai.api.rerank.RerankModelRequest;
import com.clipforge.ai.api.rerank.RerankModelResponse;
import com.clipforge.ai.api.rerank.RerankScoredItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * RerankedKnowledgeRetriever（重排序知识检索器）。
 *
 * <p>该组件位于 Hybrid Retrieval（混合检索）之后，用专用 Rerank Model（重排序模型）对候选切片进行二次排序；如果重排序关闭或失败，则回退到原 RRF（倒数排名融合）结果。
 */
@Component
@RequiredArgsConstructor
public class RerankedKnowledgeRetriever {

    /** rerankModelClient（重排序模型客户端）用于调用专用 Rerank Model（重排序模型）。 */
    private final RerankModelClient rerankModelClient;

    /** properties（重排序配置）用于读取启用开关、最大候选数和最终 TopN（保留数量）。 */
    private final RerankModelProperties properties;

    /**
     * rerankIfEnabled（按需重排序）用于在配置启用时调用 Rerank Model（重排序模型）。
     *
     * @param request 知识检索请求
     * @param chunks RRF（倒数排名融合）后的候选切片
     * @return 重排序后的切片
     */
    public List<KnowledgeRetrievedChunk> rerankIfEnabled(KnowledgeRetrievalRequest request, List<KnowledgeRetrievedChunk> chunks) {
        // 如果候选为空，则直接返回空列表。
        if (chunks == null || chunks.isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 如果未启用 Rerank（重排序），则返回原始候选。
        if (!properties.isEnabled()) {
            // 返回原始候选。
            return chunks;
        }
        try {
            // 截断送入 Rerank（重排序）模型的候选数量。
            List<KnowledgeRetrievedChunk> candidates = chunks.stream()
                    // 限制最大候选数。
                    .limit(resolveMaxCandidates())
                    // 收集为列表。
                    .toList();
            // 构造模型请求。
            RerankModelRequest modelRequest = RerankModelRequest.builder()
                    // 设置查询文本。
                    .query(request.getQuery())
                    // 设置候选文档。
                    .candidates(toRerankCandidates(candidates))
                    // 设置最终保留数量，当前默认 5。
                    .topN(resolveTopN())
                    // 设置返回文档，便于调试。
                    .returnDocuments(true)
                    // 构建请求。
                    .build();
            // 调用专用 Rerank Model（重排序模型）。
            RerankModelResponse response = rerankModelClient.rerank(modelRequest);
            // 合并重排序结果。
            List<KnowledgeRetrievedChunk> rerankedChunks = mergeRerankResults(candidates, response);
            // 如果模型没有返回有效结果，则回退原候选。
            if (rerankedChunks.isEmpty()) {
                // 返回截断后的原始候选。
                return fallbackChunks(chunks);
            }
            // 返回重排序结果。
            return rerankedChunks;
        } catch (RuntimeException exception) {
            // Rerank（重排序）失败不能影响主检索链路，直接回退截断后的 RRF（倒数排名融合）结果。
            return fallbackChunks(chunks);
        }
    }

    /**
     * finalTopK（最终返回数量）用于让统一检索结果展示真实返回数量上限。
     *
     * @param originalTopK 原始检索数量
     * @return 最终返回数量
     */
    public int finalTopK(int originalTopK) {
        // 如果未启用 Rerank（重排序），则保留原始 TopK（前 K 个）。
        if (!properties.isEnabled()) {
            // 返回原始数量。
            return originalTopK;
        }
        // 启用 Rerank（重排序）时返回 TopN（保留数量），当前默认 5。
        return Math.min(resolveTopN(), Math.max(originalTopK, 1));
    }

    /**
     * fallbackChunks（降级候选）用于在 Rerank（重排序）失败时保留 TopN（保留数量）条 RRF（倒数排名融合）结果。
     *
     * @param chunks 原始候选
     * @return 截断后的候选
     */
    private List<KnowledgeRetrievedChunk> fallbackChunks(List<KnowledgeRetrievedChunk> chunks) {
        // 截断到最终 TopN（保留数量）。
        return chunks.stream()
                // 限制数量。
                .limit(resolveTopN())
                // 收集为列表。
                .toList();
    }

    /**
     * toRerankCandidates（转换重排序候选）用于把知识切片转换为模型输入文档。
     *
     * @param chunks 知识切片列表
     * @return 重排序候选列表
     */
    private List<RerankCandidate> toRerankCandidates(List<KnowledgeRetrievedChunk> chunks) {
        // 创建候选列表。
        List<RerankCandidate> candidates = new ArrayList<>();
        // 遍历切片。
        for (int index = 0; index < chunks.size(); index++) {
            // 读取当前切片。
            KnowledgeRetrievedChunk chunk = chunks.get(index);
            // 构造候选文档。
            candidates.add(RerankCandidate.builder()
                    // 设置索引。
                    .index(index)
                    // 设置候选编号。
                    .candidateId(chunk.getChunkId())
                    // 设置候选文本。
                    .text(buildRerankText(chunk))
                    // 构建对象。
                    .build());
        }
        // 返回候选列表。
        return candidates;
    }

    /**
     * mergeRerankResults（合并重排序结果）用于把模型分数写回知识切片。
     *
     * @param candidates 原始候选切片
     * @param response 重排序模型响应
     * @return 重排序后的切片
     */
    private List<KnowledgeRetrievedChunk> mergeRerankResults(List<KnowledgeRetrievedChunk> candidates, RerankModelResponse response) {
        // 如果响应为空，则返回空列表。
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 按切片编号建立候选映射。
        Map<String, KnowledgeRetrievedChunk> chunkMap = new LinkedHashMap<>();
        // 遍历候选。
        for (KnowledgeRetrievedChunk chunk : candidates) {
            // 写入映射。
            chunkMap.put(chunk.getChunkId(), chunk);
        }
        // 创建重排序结果列表。
        List<KnowledgeRetrievedChunk> rerankedChunks = new ArrayList<>();
        // 遍历模型结果。
        for (int rank = 0; rank < response.getResults().size(); rank++) {
            // 读取重排序得分项。
            RerankScoredItem scoredItem = response.getResults().get(rank);
            // 根据候选编号优先查找切片。
            KnowledgeRetrievedChunk chunk = chunkMap.get(scoredItem.getCandidateId());
            // 如果候选编号缺失，则按 index（索引）回退查找。
            if (chunk == null && scoredItem.getIndex() != null && scoredItem.getIndex() >= 0 && scoredItem.getIndex() < candidates.size()) {
                // 按索引读取切片。
                chunk = candidates.get(scoredItem.getIndex());
            }
            // 如果仍未找到，则跳过。
            if (chunk == null) {
                // 继续下一个结果。
                continue;
            }
            // 写入重排序分数。
            chunk.setRerankScore(scoredItem.getRelevanceScore());
            // 写入重排序序号。
            chunk.setRerankIndex(rank);
            // 最终分数使用重排序分数。
            chunk.setScore(scoredItem.getRelevanceScore());
            // 加入结果列表。
            rerankedChunks.add(chunk);
        }
        // 按重排序分数倒序稳定排序。
        return rerankedChunks.stream()
                // 分数越大越靠前。
                .sorted(Comparator.comparing(KnowledgeRetrievedChunk::getRerankScore, Comparator.nullsLast(Comparator.reverseOrder())))
                // 截断到 TopN（保留数量）。
                .limit(resolveTopN())
                // 收集为列表。
                .toList();
    }

    /**
     * buildRerankText（构建重排序文本）用于把标题路径和正文一起送入模型。
     *
     * @param chunk 知识切片
     * @return 重排序文本
     */
    private String buildRerankText(KnowledgeRetrievedChunk chunk) {
        // 拼接标题路径和正文，帮助 Cross-Encoder（交叉编码器）理解章节语义。
        return "标题路径：" + blankToDefault(chunk.getTitlePath(), "未提供") + "\n内容：" + blankToDefault(chunk.getContent(), "无内容");
    }

    /**
     * resolveTopN（解析保留数量）用于读取最终保留数量，当前默认 5。
     *
     * @return 保留数量
     */
    private int resolveTopN() {
        // 如果配置非法，则使用 5。
        if (properties.getTopN() == null || properties.getTopN() <= 0) {
            // 返回默认值。
            return 5;
        }
        // 返回配置值。
        return properties.getTopN();
    }

    /**
     * resolveMaxCandidates（解析最大候选数）用于控制送入 Rerank Model（重排序模型）的候选数量。
     *
     * @return 最大候选数
     */
    private int resolveMaxCandidates() {
        // 如果配置非法，则使用 20。
        if (properties.getMaxCandidates() == null || properties.getMaxCandidates() <= 0) {
            // 返回默认候选数量。
            return 20;
        }
        // 返回配置值。
        return properties.getMaxCandidates();
    }

    /**
     * blankToDefault（空白转默认值）用于构造模型输入文本。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 非空文本
     */
    private String blankToDefault(String value, String defaultValue) {
        // 如果原始值为空，则返回默认值。
        if (value == null || value.isBlank()) {
            // 返回默认值。
            return defaultValue;
        }
        // 返回原始值。
        return value;
    }
}
