package com.clipforge.rag.retrieval;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * RrfHybridRanker（RRF 混合排序器）。
 *
 * <p>该组件用于 Phase 7.10.4（阶段 7.10.4）把向量检索候选和 BM25（最佳匹配 25）关键词检索候选做 RRF（Reciprocal Rank Fusion，倒数排名融合）。
 */
@Component
@RequiredArgsConstructor
public class RrfHybridRanker {

    /** properties（知识检索配置）用于读取 RRF（倒数排名融合）参数。 */
    private final KnowledgeRetrievalProperties properties;

    /**
     * rank（排序）用于融合向量候选和关键词候选。
     *
     * @param vectorChunks 向量检索候选
     * @param keywordChunks 关键词检索候选
     * @param topK 最终返回数量
     * @return 融合排序后的候选列表
     */
    public List<KnowledgeRetrievedChunk> rank(List<KnowledgeRetrievedChunk> vectorChunks,
                                              List<KnowledgeRetrievedChunk> keywordChunks,
                                              int topK) {
        // 创建按 chunkId（切片编号）去重的候选映射。
        Map<String, FusionCandidate> candidateMap = new LinkedHashMap<>();
        // 追加向量候选。
        appendCandidates(candidateMap, safeList(vectorChunks), KnowledgeRetrievalSource.PGVECTOR);
        // 追加关键词候选。
        appendCandidates(candidateMap, safeList(keywordChunks), KnowledgeRetrievalSource.KEYWORD);
        // 如果没有任何候选，则返回空列表。
        if (candidateMap.isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 返回融合排序结果。
        return candidateMap.values().stream()
                // 转换为统一知识切片。
                .map(FusionCandidate::toChunk)
                // 按 RRF（倒数排名融合）分数倒序排列。
                .sorted(Comparator.comparing(KnowledgeRetrievedChunk::getRrfScore, Comparator.nullsLast(Comparator.reverseOrder())))
                // 截断到 TopK（前 K 个）。
                .limit(Math.max(topK, 1))
                // 收集为列表。
                .toList();
    }

    /**
     * appendCandidates（追加候选）用于把某一路检索候选写入融合映射。
     *
     * @param candidateMap 候选映射
     * @param chunks 当前检索候选
     * @param source 当前候选来源
     */
    private void appendCandidates(Map<String, FusionCandidate> candidateMap,
                                  List<KnowledgeRetrievedChunk> chunks,
                                  KnowledgeRetrievalSource source) {
        // 遍历候选列表。
        for (int index = 0; index < chunks.size(); index++) {
            // 读取当前候选。
            KnowledgeRetrievedChunk chunk = chunks.get(index);
            // 如果候选为空或切片编号为空，则跳过。
            if (chunk == null || chunk.getChunkId() == null || chunk.getChunkId().isBlank()) {
                // 继续下一个候选。
                continue;
            }
            // 计算当前候选的 rank（排名），从 1 开始。
            int rank = index + 1;
            // 计算当前候选的 RRF（倒数排名融合）贡献分。
            double contribution = calculateRrfContribution(rank);
            // 获取或创建融合候选。
            FusionCandidate candidate = candidateMap.computeIfAbsent(chunk.getChunkId(), ignored -> new FusionCandidate(chunk));
            // 追加当前来源的候选信息。
            candidate.append(chunk, source, contribution);
        }
    }

    /**
     * calculateRrfContribution（计算 RRF 贡献）用于根据单路排名计算融合贡献分。
     *
     * @param rank 当前候选在单路检索中的排名
     * @return RRF（倒数排名融合）贡献分
     */
    private double calculateRrfContribution(int rank) {
        // 读取配置中的 k（排名平滑参数）。
        int k = Math.max(properties.getRrf().getK(), 1);
        // 返回 RRF（倒数排名融合）贡献分。
        return 1.0 / (k + Math.max(rank, 1));
    }

    /**
     * safeList（安全列表）用于把空列表转成可遍历列表。
     *
     * @param chunks 原始候选列表
     * @return 非空候选列表
     */
    private List<KnowledgeRetrievedChunk> safeList(List<KnowledgeRetrievedChunk> chunks) {
        // 如果原始列表为空，则返回空列表。
        if (chunks == null || chunks.isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 返回原始列表。
        return chunks;
    }

    /**
     * FusionCandidate（融合候选）。
     *
     * <p>该内部对象用于累积同一个 chunkId（切片编号）在向量检索和关键词检索中的分数、来源和 RRF（倒数排名融合）贡献。
     */
    private static class FusionCandidate {

        /** baseChunk（基础切片）用于保留标题、正文、文档编号等展示信息。 */
        private KnowledgeRetrievedChunk baseChunk;

        /** vectorHit（向量命中）表示当前切片是否被向量检索召回。 */
        private boolean vectorHit;

        /** keywordHit（关键词命中）表示当前切片是否被关键词检索召回。 */
        private boolean keywordHit;

        /** vectorScore（向量分数）保存向量检索原始分数。 */
        private Double vectorScore;

        /** keywordScore（关键词分数）保存 BM25（最佳匹配 25）原始分数。 */
        private Double keywordScore;

        /** rrfScore（RRF 分数）保存融合分数。 */
        private double rrfScore;

        /**
         * FusionCandidate（融合候选构造函数）。
         *
         * @param baseChunk 基础切片
         */
        private FusionCandidate(KnowledgeRetrievedChunk baseChunk) {
            // 保存基础切片。
            this.baseChunk = baseChunk;
        }

        /**
         * append（追加候选）用于累加某一路检索结果。
         *
         * @param chunk 当前切片
         * @param source 当前来源
         * @param contribution RRF（倒数排名融合）贡献分
         */
        private void append(KnowledgeRetrievedChunk chunk, KnowledgeRetrievalSource source, double contribution) {
            // 累加 RRF（倒数排名融合）分数。
            rrfScore += contribution;
            // 如果当前来源是向量检索，则记录向量信息。
            if (source == KnowledgeRetrievalSource.PGVECTOR) {
                // 标记向量命中。
                vectorHit = true;
                // 记录向量分数。
                vectorScore = firstNonNull(chunk.getVectorScore(), chunk.getScore());
            }
            // 如果当前来源是关键词检索，则记录关键词信息。
            if (source == KnowledgeRetrievalSource.KEYWORD) {
                // 标记关键词命中。
                keywordHit = true;
                // 记录关键词分数。
                keywordScore = firstNonNull(chunk.getKeywordScore(), chunk.getScore());
            }
            // 如果当前候选正文更完整，则更新基础切片。
            if (isBetterBaseChunk(chunk, baseChunk)) {
                // 更新基础切片。
                baseChunk = chunk;
            }
        }

        /**
         * toChunk（转切片）用于把融合候选转换为统一知识切片。
         *
         * @return 融合后的知识切片
         */
        private KnowledgeRetrievedChunk toChunk() {
            // 判断最终来源。
            KnowledgeRetrievalSource source = vectorHit && keywordHit ? KnowledgeRetrievalSource.HYBRID : (vectorHit ? KnowledgeRetrievalSource.PGVECTOR : KnowledgeRetrievalSource.KEYWORD);
            // 使用 Builder（构建器）创建融合后的切片。
            return KnowledgeRetrievedChunk.builder()
                    // 设置来源。
                    .source(source)
                    // 设置知识库编号。
                    .knowledgeBaseId(baseChunk.getKnowledgeBaseId())
                    // 设置文档编号。
                    .documentId(baseChunk.getDocumentId())
                    // 设置切片编号。
                    .chunkId(baseChunk.getChunkId())
                    // 设置切片序号。
                    .chunkIndex(baseChunk.getChunkIndex())
                    // 设置标题路径。
                    .titlePath(baseChunk.getTitlePath())
                    // 设置正文。
                    .content(baseChunk.getContent())
                    // 最终排序分数使用 RRF（倒数排名融合）分数。
                    .score(rrfScore)
                    // 设置向量原始分数。
                    .vectorScore(vectorScore)
                    // 设置关键词原始分数。
                    .keywordScore(keywordScore)
                    // 设置 RRF（倒数排名融合）分数。
                    .rrfScore(rrfScore)
                    // 保留向量距离。
                    .distance(baseChunk.getDistance())
                    // 保留元数据。
                    .metadataJson(baseChunk.getMetadataJson())
                    // 构建对象。
                    .build();
        }

        /**
         * firstNonNull（取第一个非空值）用于兼容旧候选对象未设置专用分数字段的情况。
         *
         * @param first 第一个值
         * @param second 第二个值
         * @return 非空值
         */
        private Double firstNonNull(Double first, Double second) {
            // 如果第一个值不为空，则返回第一个值。
            if (first != null) {
                // 返回第一个值。
                return first;
            }
            // 返回第二个值。
            return second;
        }

        /**
         * isBetterBaseChunk（是否更适合作为基础切片）用于选择信息更完整的候选承载展示字段。
         *
         * @param current 当前候选
         * @param existing 已有候选
         * @return true 表示当前候选更适合
         */
        private boolean isBetterBaseChunk(KnowledgeRetrievedChunk current, KnowledgeRetrievedChunk existing) {
            // 如果已有候选为空，则当前候选更好。
            if (existing == null) {
                // 返回 true。
                return true;
            }
            // 当前正文长度。
            int currentLength = current.getContent() == null ? 0 : current.getContent().length();
            // 已有正文长度。
            int existingLength = existing.getContent() == null ? 0 : existing.getContent().length();
            // 正文更长通常意味着可注入上下文更完整。
            return currentLength > existingLength;
        }
    }
}
