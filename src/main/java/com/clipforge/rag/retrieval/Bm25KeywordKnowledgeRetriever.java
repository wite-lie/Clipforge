package com.clipforge.rag.retrieval;

import com.clipforge.infra.persistence.entity.KnowledgeChunkEntity;
import com.clipforge.infra.persistence.repository.KnowledgeChunkRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Bm25KeywordKnowledgeRetriever（BM25 关键词知识检索器）。
 *
 * <p>该实现基于 cf_knowledge_chunk（知识切片表）动态计算 BM25（最佳匹配 25）分数，不依赖 Elasticsearch（搜索引擎）。
 */
@Component
@RequiredArgsConstructor
public class Bm25KeywordKnowledgeRetriever implements KeywordKnowledgeRetriever {

    /** keywordTokenizer（关键词分词器）用于把查询文本拆成可检索词项。 */
    private final KeywordTokenizer keywordTokenizer;

    /** knowledgeChunkRepositoryProvider（知识切片仓储提供器）用于兼容未启用 db（数据库）Profile（配置环境）的场景。 */
    private final ObjectProvider<KnowledgeChunkRepository> knowledgeChunkRepositoryProvider;

    /** properties（知识检索配置）用于读取 BM25（最佳匹配 25）参数。 */
    private final KnowledgeRetrievalProperties properties;

    /**
     * retrieveCandidates（检索候选知识）用于执行 BM25（最佳匹配 25）关键词召回。
     *
     * @param request 知识检索请求
     * @param topK 最多召回多少条候选
     * @return 关键词候选切片列表
     */
    @Override
    public List<KnowledgeRetrievedChunk> retrieveCandidates(KnowledgeRetrievalRequest request, int topK) {
        // BM25（最佳匹配 25）未启用时直接返回空候选。
        if (!properties.getBm25().isEnabled()) {
            return List.of();
        }
        // 请求为空或查询文本为空时无法检索。
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            return List.of();
        }
        // 没有指定知识库编号且没有开启全库检索时，把降级决策交给统一检索入口。
        if ((request.getKnowledgeBaseIds() == null || request.getKnowledgeBaseIds().isEmpty())
                && !Boolean.TRUE.equals(request.getSearchAllKnowledgeBases())) {
            return List.of();
        }
        // 未启用数据库 Profile（配置环境）时没有切片仓储，直接返回空候选。
        KnowledgeChunkRepository repository = knowledgeChunkRepositoryProvider.getIfAvailable();
        if (repository == null) {
            return List.of();
        }
        // 拆分查询词项。
        List<String> queryTerms = keywordTokenizer.tokenize(request.getQuery());
        if (queryTerms.isEmpty()) {
            return List.of();
        }
        // 全库检索时读取全部有效切片；否则只读取指定知识库下的有效切片。
        List<KnowledgeChunkEntity> chunks = Boolean.TRUE.equals(request.getSearchAllKnowledgeBases())
                ? repository.listAllEnabled()
                : repository.listEnabledByKnowledgeBaseIds(request.getKnowledgeBaseIds());
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        // 计算 BM25（最佳匹配 25）所需的平均文档长度和文档频率。
        double averageDocumentLength = calculateAverageDocumentLength(chunks);
        Map<String, Integer> documentFrequency = calculateDocumentFrequency(queryTerms, chunks);
        // 逐切片打分并返回最高分候选。
        return chunks.stream()
                .map(chunk -> toScoredChunk(chunk, queryTerms, documentFrequency, chunks.size(), averageDocumentLength))
                .filter(chunk -> chunk.getScore() != null && chunk.getScore() > 0)
                .sorted(Comparator.comparing(KnowledgeRetrievedChunk::getScore, Comparator.reverseOrder()))
                .limit(resolveCandidateLimit(topK))
                .toList();
    }

    /**
     * calculateDocumentFrequency（计算文档频率）用于统计每个查询词命中了多少个切片。
     *
     * @param queryTerms 查询词项
     * @param chunks 候选切片
     * @return 查询词到文档频率的映射
     */
    private Map<String, Integer> calculateDocumentFrequency(List<String> queryTerms, List<KnowledgeChunkEntity> chunks) {
        // 使用 Map（映射）保存每个词项命中的切片数量。
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (KnowledgeChunkEntity chunk : chunks) {
            String searchableText = normalizeSearchableText(chunk);
            Set<String> matchedTerms = new HashSet<>();
            for (String term : queryTerms) {
                if (containsTerm(searchableText, term)) {
                    matchedTerms.add(term);
                }
            }
            for (String term : matchedTerms) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }
        return documentFrequency;
    }

    /**
     * toScoredChunk（转换为带分切片）用于把数据库切片转换成统一检索结果。
     *
     * @param chunk 数据库知识切片
     * @param queryTerms 查询词项
     * @param documentFrequency 文档频率
     * @param totalDocuments 总切片数
     * @param averageDocumentLength 平均切片长度
     * @return 带分知识切片
     */
    private KnowledgeRetrievedChunk toScoredChunk(KnowledgeChunkEntity chunk,
                                                 List<String> queryTerms,
                                                 Map<String, Integer> documentFrequency,
                                                 int totalDocuments,
                                                 double averageDocumentLength) {
        // 合并标题路径和正文后计算关键词分数。
        String searchableText = normalizeSearchableText(chunk);
        int documentLength = Math.max(searchableText.length(), 1);
        double score = 0.0;
        for (String term : queryTerms) {
            int termFrequency = countTermFrequency(searchableText, term);
            if (termFrequency <= 0) {
                continue;
            }
            int df = documentFrequency.getOrDefault(term, 0);
            if (df <= 0) {
                continue;
            }
            score += calculateTermScore(termFrequency, df, totalDocuments, documentLength, averageDocumentLength);
        }
        return KnowledgeRetrievedChunk.builder()
                .source(KnowledgeRetrievalSource.KEYWORD)
                .knowledgeBaseId(chunk.getKnowledgeBaseId())
                .documentId(chunk.getDocumentId())
                .chunkId(chunk.getChunkId())
                .chunkIndex(chunk.getChunkIndex())
                .titlePath(chunk.getTitlePath())
                .content(chunk.getContent())
                .score(score)
                .keywordScore(score)
                .distance(null)
                .metadataJson(chunk.getMetadataJson())
                .build();
    }

    /**
     * calculateTermScore（计算单词项分数）用于执行 BM25（最佳匹配 25）公式。
     *
     * @param termFrequency tf（词频）
     * @param documentFrequency df（文档频率）
     * @param totalDocuments N（总文档数）
     * @param documentLength dl（当前文档长度）
     * @param averageDocumentLength avgdl（平均文档长度）
     * @return 单个词项的 BM25（最佳匹配 25）分数
     */
    private double calculateTermScore(int termFrequency,
                                      int documentFrequency,
                                      int totalDocuments,
                                      int documentLength,
                                      double averageDocumentLength) {
        // k1（词频饱和参数）和 b（长度归一化参数）来自配置。
        double k1 = properties.getBm25().getK1();
        double b = properties.getBm25().getB();
        double idf = Math.log(1.0 + (totalDocuments - documentFrequency + 0.5) / (documentFrequency + 0.5));
        double denominator = termFrequency + k1 * (1.0 - b + b * documentLength / Math.max(averageDocumentLength, 1.0));
        return idf * (termFrequency * (k1 + 1.0)) / denominator;
    }

    /**
     * calculateAverageDocumentLength（计算平均文档长度）用于获得 BM25（最佳匹配 25）的 avgdl（平均文档长度）。
     *
     * @param chunks 候选切片列表
     * @return 平均文档长度
     */
    private double calculateAverageDocumentLength(List<KnowledgeChunkEntity> chunks) {
        // 用标题路径和正文长度近似表示切片长度。
        int totalLength = chunks.stream()
                .mapToInt(chunk -> Math.max(normalizeSearchableText(chunk).length(), 1))
                .sum();
        return (double) totalLength / Math.max(chunks.size(), 1);
    }

    /**
     * normalizeSearchableText（归一化可检索文本）用于合并标题路径和正文，并统一大小写。
     *
     * @param chunk 知识切片
     * @return 可检索文本
     */
    private String normalizeSearchableText(KnowledgeChunkEntity chunk) {
        // 标题路径也参与关键词检索，可以增强章节标题命中。
        String text = blankToEmpty(chunk.getTitlePath()) + "\n" + blankToEmpty(chunk.getContent());
        return text.toLowerCase(Locale.ROOT);
    }

    /**
     * countTermFrequency（计算词频）用于统计词项在切片文本中出现的次数。
     *
     * @param searchableText 可检索文本
     * @param term 查询词项
     * @return 词频
     */
    private int countTermFrequency(String searchableText, String term) {
        // 空词项没有检索意义。
        if (term == null || term.isBlank()) {
            return 0;
        }
        String normalizedTerm = term.toLowerCase(Locale.ROOT);
        int count = 0;
        int fromIndex = 0;
        while (fromIndex < searchableText.length()) {
            int index = searchableText.indexOf(normalizedTerm, fromIndex);
            if (index < 0) {
                break;
            }
            count++;
            fromIndex = index + 1;
        }
        return count;
    }

    /**
     * containsTerm（是否包含词项）用于计算 df（文档频率）。
     *
     * @param searchableText 可检索文本
     * @param term 查询词项
     * @return true 表示包含
     */
    private boolean containsTerm(String searchableText, String term) {
        // 复用词频统计判断是否命中。
        return countTermFrequency(searchableText, term) > 0;
    }

    /**
     * resolveCandidateLimit（解析候选限制）用于合并请求 TopK（前 K 个）和 BM25（最佳匹配 25）候选配置。
     *
     * @param topK 本次请求数量
     * @return 实际候选数量
     */
    private int resolveCandidateLimit(int topK) {
        // 配置和请求数量都至少为 1。
        int configuredLimit = Math.max(properties.getBm25().getCandidateLimit(), 1);
        int requestLimit = Math.max(topK, 1);
        return Math.min(configuredLimit, requestLimit);
    }

    /**
     * blankToEmpty（空白转空字符串）用于减少空值判断。
     *
     * @param value 原始文本
     * @return 非空文本
     */
    private String blankToEmpty(String value) {
        // 空白值统一转为空字符串。
        if (value == null || value.isBlank()) {
            return "";
        }
        return value;
    }
}
