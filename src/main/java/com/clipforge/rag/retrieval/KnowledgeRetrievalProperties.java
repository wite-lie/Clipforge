package com.clipforge.rag.retrieval;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KnowledgeRetrievalProperties（知识检索配置）。
 *
 * <p>该配置用于控制 Phase 7.9（阶段 7.9）RAG（检索增强生成）检索服务的默认 TopK（前 K 个）数量和静态降级开关。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.rag.retrieval")
public class KnowledgeRetrievalProperties {

    /** defaultTopK（默认返回数量）表示没有显式传入 topK（前 K 个）时返回多少条候选知识，当前按用户要求默认为 15。 */
    private int defaultTopK = 15;

    /** staticFallbackEnabled（启用静态降级）表示向量检索不可用或无结果时是否回退到本地 Markdown（标记文档）静态知识库。 */
    private boolean staticFallbackEnabled = true;

    /** hybridEnabled（启用混合检索）表示是否启用向量检索、关键词检索和 RRF（倒数排名融合）的混合检索链路。 */
    private boolean hybridEnabled = true;

    /** bm25（最佳匹配 25）配置用于控制关键词检索的打分参数。 */
    private Bm25 bm25 = new Bm25();

    /** rrf（倒数排名融合）配置用于控制混合检索融合排序。 */
    private Rrf rrf = new Rrf();

    /**
     * Bm25（最佳匹配 25 配置）。
     *
     * <p>这些参数控制关键词检索对词频和文档长度的敏感程度，第一版使用通用默认值。
     */
    @Getter
    @Setter
    public static class Bm25 {

        /** enabled（是否启用）表示是否启用 BM25（最佳匹配 25）关键词检索。 */
        private boolean enabled = true;

        /** k1（词频饱和参数）控制词频增长带来的分数增益，常见默认值为 1.2 到 2.0。 */
        private double k1 = 1.5;

        /** b（长度归一化参数）控制长文本惩罚强度，常见默认值为 0.75。 */
        private double b = 0.75;

        /** candidateLimit（候选数量限制）表示 BM25（最佳匹配 25）最多返回多少条候选，避免后续融合阶段候选过多。 */
        private int candidateLimit = 200;
    }

    /**
     * Rrf（倒数排名融合配置）。
     *
     * <p>该配置用于控制向量候选和关键词候选融合时的排名衰减强度。
     */
    @Getter
    @Setter
    public static class Rrf {

        /** k（排名平滑参数）表示 RRF（倒数排名融合）公式中的平滑常数，常见默认值为 60。 */
        private int k = 60;
    }
}
