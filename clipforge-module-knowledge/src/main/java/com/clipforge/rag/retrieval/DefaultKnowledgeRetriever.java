package com.clipforge.rag.retrieval;

import com.clipforge.knowledge.retrieval.KnowledgeRetrievalProperties;
import com.clipforge.knowledge.retrieval.KeywordTokenizer;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalRequest;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalResult;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalSource;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievedChunk;
import com.clipforge.knowledge.retrieval.KnowledgeRetriever;
import com.clipforge.knowledge.retrieval.RrfHybridRanker;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.rag.StaticCreativeKnowledgeRetriever;
import com.clipforge.knowledge.evidence.RagEvidenceAssembler;
import com.clipforge.knowledge.evidence.RagEvidenceAssemblyResult;
import com.clipforge.knowledge.evidence.RagEvidenceProperties;
import com.clipforge.creation.knowledge.CreativeKnowledge;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * DefaultKnowledgeRetriever（默认知识检索器）。
 *
 * <p>该实现是 RAG（检索增强生成）统一入口：默认使用 HybridKnowledgeRetriever（混合知识检索器），Rerank（重排序）后再通过
 * RagEvidenceAssembler（RAG 证据组装器）生成可注入 Prompt（提示词）的证据区。
 */
@Component
public class DefaultKnowledgeRetriever implements KnowledgeRetriever {

    /** hybridKnowledgeRetriever（混合知识检索器）用于执行向量检索、BM25（最佳匹配 25）关键词检索和 RRF（倒数排名融合）。 */
    private final HybridKnowledgeRetriever hybridKnowledgeRetriever;

    /** staticCreativeKnowledgeRetriever（静态创作知识检索器）用于在真实 RAG（检索增强生成）不可用时读取本地 Markdown（标记文档）知识。 */
    private final StaticCreativeKnowledgeRetriever staticCreativeKnowledgeRetriever;

    /** properties（知识检索配置）用于读取默认 topK（前 K 个）和静态降级开关。 */
    private final KnowledgeRetrievalProperties properties;

    /** rerankedKnowledgeRetriever（重排序知识检索器）用于在混合检索后按需执行专用 Rerank（重排序）。 */
    private final RerankedKnowledgeRetriever rerankedKnowledgeRetriever;

    /** ragEvidenceAssembler（RAG 证据组装器）用于控制最终进入 Prompt（提示词）的证据格式和预算。 */
    private final RagEvidenceAssembler ragEvidenceAssembler;

    /**
     * 兼容构造函数用于已有单元测试，默认创建一套证据组装配置。
     *
     * @param hybridKnowledgeRetriever 混合知识检索器
     * @param staticCreativeKnowledgeRetriever 静态创作知识检索器
     * @param properties 知识检索配置
     * @param rerankedKnowledgeRetriever 重排序知识检索器
     */
    public DefaultKnowledgeRetriever(HybridKnowledgeRetriever hybridKnowledgeRetriever,
                                     StaticCreativeKnowledgeRetriever staticCreativeKnowledgeRetriever,
                                     KnowledgeRetrievalProperties properties,
                                     RerankedKnowledgeRetriever rerankedKnowledgeRetriever) {
        this(hybridKnowledgeRetriever, staticCreativeKnowledgeRetriever, properties, rerankedKnowledgeRetriever, new RagEvidenceAssembler(new RagEvidenceProperties()));
    }

    /**
     * 完整构造函数用于 Spring（Spring 框架）注入全部依赖。
     *
     * @param hybridKnowledgeRetriever 混合知识检索器
     * @param staticCreativeKnowledgeRetriever 静态创作知识检索器
     * @param properties 知识检索配置
     * @param rerankedKnowledgeRetriever 重排序知识检索器
     * @param ragEvidenceAssembler RAG 证据组装器
     */
    @Autowired
    public DefaultKnowledgeRetriever(HybridKnowledgeRetriever hybridKnowledgeRetriever,
                                     StaticCreativeKnowledgeRetriever staticCreativeKnowledgeRetriever,
                                     KnowledgeRetrievalProperties properties,
                                     RerankedKnowledgeRetriever rerankedKnowledgeRetriever,
                                     RagEvidenceAssembler ragEvidenceAssembler) {
        this.hybridKnowledgeRetriever = hybridKnowledgeRetriever;
        this.staticCreativeKnowledgeRetriever = staticCreativeKnowledgeRetriever;
        this.properties = properties;
        this.rerankedKnowledgeRetriever = rerankedKnowledgeRetriever;
        this.ragEvidenceAssembler = ragEvidenceAssembler;
    }

    /**
     * retrieve（检索）用于执行混合检索、重排序和证据组装，并在必要时静态降级。
     *
     * @param request 知识检索请求
     * @return 知识检索结果
     */
    @Override
    public KnowledgeRetrievalResult retrieve(KnowledgeRetrievalRequest request) {
        validateRequest(request);
        int topK = resolveTopK(request);
        if ((request.getKnowledgeBaseIds() == null || request.getKnowledgeBaseIds().isEmpty())
                && !Boolean.TRUE.equals(request.getSearchAllKnowledgeBases())) {
            return fallback(request, topK, "请求没有传入 knowledgeBaseIds（知识库编号列表），且没有开启全库检索");
        }
        try {
            List<KnowledgeRetrievedChunk> retrievedChunks = retrieveKnowledgeChunks(request, topK);
            List<KnowledgeRetrievedChunk> topChunks = rerankedKnowledgeRetriever.rerankIfEnabled(request, retrievedChunks);
            if (topChunks.isEmpty()) {
                return fallback(request, topK, "RAG（检索增强生成）检索没有命中结果");
            }
            RagEvidenceAssemblyResult evidenceResult = ragEvidenceAssembler.assemble(topChunks);
            return KnowledgeRetrievalResult.builder()
                    .query(request.getQuery())
                    .topK(rerankedKnowledgeRetriever.finalTopK(topK))
                    .source(resolveResultSource(topChunks))
                    .fallbackUsed(false)
                    .fallbackReason(null)
                    .chunks(topChunks)
                    .promptEvidences(evidenceResult.getPromptEvidences())
                    .evidenceAssemblyResult(evidenceResult)
                    .promptContext(evidenceResult.getPromptContext())
                    .build();
        } catch (RuntimeException exception) {
            if (!fallbackEnabled(request)) {
                throw exception;
            }
            return fallback(request, topK, "RAG（检索增强生成）检索异常：" + exception.getMessage());
        }
    }

    /**
     * retrieveKnowledgeChunks（检索知识切片）用于根据配置选择混合检索或向量检索。
     *
     * @param request 知识检索请求
     * @param topK 返回数量
     * @return 知识切片列表
     */
    private List<KnowledgeRetrievedChunk> retrieveKnowledgeChunks(KnowledgeRetrievalRequest request, int topK) {
        if (properties.isHybridEnabled()) {
            return hybridKnowledgeRetriever.retrieveHybridCandidates(request, topK);
        }
        return hybridKnowledgeRetriever.retrieveVectorCandidates(request, topK);
    }

    /**
     * resolveResultSource（解析结果来源）用于根据最终切片列表确定本次检索主来源。
     *
     * @param chunks 最终知识切片
     * @return 检索来源
     */
    private KnowledgeRetrievalSource resolveResultSource(List<KnowledgeRetrievedChunk> chunks) {
        if (chunks.stream().anyMatch(chunk -> chunk.getSource() == KnowledgeRetrievalSource.HYBRID)) {
            return KnowledgeRetrievalSource.HYBRID;
        }
        if (chunks.stream().anyMatch(chunk -> chunk.getSource() == KnowledgeRetrievalSource.KEYWORD)) {
            return KnowledgeRetrievalSource.KEYWORD;
        }
        if (chunks.stream().anyMatch(chunk -> chunk.getSource() == KnowledgeRetrievalSource.STATIC_FALLBACK)) {
            return KnowledgeRetrievalSource.STATIC_FALLBACK;
        }
        return KnowledgeRetrievalSource.PGVECTOR;
    }

    /**
     * fallback（静态降级）用于把静态创作知识包装成统一检索结果。
     *
     * @param request 检索请求
     * @param topK 返回数量
     * @param reason 降级原因
     * @return 静态降级结果
     */
    private KnowledgeRetrievalResult fallback(KnowledgeRetrievalRequest request, int topK, String reason) {
        if (!fallbackEnabled(request)) {
            RagEvidenceAssemblyResult emptyEvidenceResult = ragEvidenceAssembler.assemble(List.of());
            return KnowledgeRetrievalResult.builder()
                    .query(request.getQuery())
                    .topK(topK)
                    .source(KnowledgeRetrievalSource.PGVECTOR)
                    .fallbackUsed(false)
                    .fallbackReason(reason)
                    .chunks(List.of())
                    .promptEvidences(emptyEvidenceResult.getPromptEvidences())
                    .evidenceAssemblyResult(emptyEvidenceResult)
                    .promptContext("")
                    .build();
        }
        CreativeKnowledge knowledge = staticCreativeKnowledgeRetriever.retrieveByPlatform(resolvePlatform(request));
        List<KnowledgeRetrievedChunk> chunks = toStaticChunks(knowledge, topK);
        RagEvidenceAssemblyResult evidenceResult = ragEvidenceAssembler.assemble(chunks);
        return KnowledgeRetrievalResult.builder()
                .query(request.getQuery())
                .topK(topK)
                .source(KnowledgeRetrievalSource.STATIC_FALLBACK)
                .fallbackUsed(true)
                .fallbackReason(reason)
                .chunks(chunks)
                .promptEvidences(evidenceResult.getPromptEvidences())
                .evidenceAssemblyResult(evidenceResult)
                .promptContext(evidenceResult.getPromptContext())
                .build();
    }

    /**
     * toStaticChunks（转静态切片）用于把平台规则、脚本结构和评估提示转成统一切片。
     *
     * @param knowledge 静态创作知识
     * @param topK 返回数量
     * @return 静态切片列表
     */
    private List<KnowledgeRetrievedChunk> toStaticChunks(CreativeKnowledge knowledge, int topK) {
        List<KnowledgeRetrievedChunk> chunks = new ArrayList<>();
        if (knowledge == null) {
            return chunks;
        }
        appendStaticChunks(chunks, "平台规则", knowledge.getPlatformRules());
        appendStaticChunks(chunks, "脚本结构", knowledge.getScriptPatterns());
        appendStaticChunks(chunks, "评估提示", knowledge.getEvaluationHints());
        return chunks.stream()
                .limit(topK)
                .toList();
    }

    /**
     * appendStaticChunks（追加静态切片）用于把某一类静态知识加入结果列表。
     *
     * @param chunks 目标切片列表
     * @param titlePath 标题路径
     * @param items 静态知识条目
     */
    private void appendStaticChunks(List<KnowledgeRetrievedChunk> chunks, String titlePath, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (String item : items) {
            int index = chunks.size();
            chunks.add(KnowledgeRetrievedChunk.builder()
                    .source(KnowledgeRetrievalSource.STATIC_FALLBACK)
                    .knowledgeBaseId(null)
                    .documentId(null)
                    .chunkId("static_fallback_" + index)
                    .chunkIndex(index)
                    .titlePath(titlePath)
                    .content(item)
                    .score(1.0 - index * 0.01)
                    .distance(null)
                    .metadataJson(null)
                    .build());
        }
    }

    /**
     * resolveTopK（解析返回数量）用于处理请求参数和默认配置。
     *
     * @param request 检索请求
     * @return TopK（前 K 个）
     */
    private int resolveTopK(KnowledgeRetrievalRequest request) {
        if (request.getTopK() != null && request.getTopK() > 0) {
            return request.getTopK();
        }
        return Math.max(properties.getDefaultTopK(), 1);
    }

    /**
     * fallbackEnabled（是否启用降级）用于解析请求级和全局级降级开关。
     *
     * @param request 检索请求
     * @return true 表示启用静态降级
     */
    private boolean fallbackEnabled(KnowledgeRetrievalRequest request) {
        if (request.getStaticFallbackEnabled() != null) {
            return request.getStaticFallbackEnabled();
        }
        return properties.isStaticFallbackEnabled();
    }

    /**
     * resolvePlatform（解析平台）用于静态降级时选择平台知识文件。
     *
     * @param request 检索请求
     * @return 平台字符串
     */
    private String resolvePlatform(KnowledgeRetrievalRequest request) {
        if (request.getPlatform() == null || request.getPlatform().isBlank()) {
            return "default";
        }
        return request.getPlatform();
    }

    /**
     * validateRequest（校验请求）用于保证查询文本存在。
     *
     * @param request 检索请求
     */
    private void validateRequest(KnowledgeRetrievalRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识检索请求不能为空");
        }
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识检索 query（查询文本）不能为空");
        }
    }
}
