package com.clipforge.knowledge.evidence;

import com.clipforge.knowledge.retrieval.KnowledgeRetrievedChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RagEvidenceAssembler（RAG 证据组装器）。
 *
 * <p>该组件位于 Rerank（重排序）之后，负责把 TopN（前 N 条）切片转换为可追溯、去重、受预算保护的 Prompt（提示词）证据。
 */
@Component
public class RagEvidenceAssembler {

    /** properties（RAG 证据配置）用于控制证据数量和预算保护。 */
    private final RagEvidenceProperties properties;

    /** renderer（RAG 证据提示词渲染器）用于把证据对象转换成 Prompt（提示词）文本。 */
    private final RagEvidencePromptRenderer renderer;

    /**
     * 完整构造函数用于 Spring（Spring 框架）注入全部依赖。
     *
     * @param properties RAG 证据配置
     * @param renderer RAG 证据提示词渲染器
     */
    @Autowired
    public RagEvidenceAssembler(RagEvidenceProperties properties, RagEvidencePromptRenderer renderer) {
        this.properties = properties;
        this.renderer = renderer;
    }

    /**
     * 兼容构造函数用于单元测试中直接 new（创建）组装器。
     *
     * @param properties RAG 证据配置
     */
    public RagEvidenceAssembler(RagEvidenceProperties properties) {
        // 使用默认渲染器。
        this(properties, new RagEvidencePromptRenderer());
    }

    /**
     * assemble（组装）用于把检索切片转换为 Prompt（提示词）证据上下文。
     *
     * @param chunks Rerank（重排序）后的切片列表
     * @return RAG 证据组装结果
     */
    public RagEvidenceAssemblyResult assemble(List<KnowledgeRetrievedChunk> chunks) {
        // 原始切片为空时直接返回证据不足提示。
        if (chunks == null || chunks.isEmpty()) {
            String promptContext = renderer.render(List.of(), properties.isInsufficientEvidenceMarkerEnabled());
            return RagEvidenceAssemblyResult.builder()
                    .promptContext(promptContext)
                    .promptEvidences(List.of())
                    .filteredEvidences(List.of())
                    .budgetProtectionTriggered(false)
                    .originalChunkCount(0)
                    .promptEvidenceCount(0)
                    .promptContextChars(promptContext.length())
                    .build();
        }
        // 先按规则过滤空内容、重复切片和同文档过量证据。
        EvidenceSelection selection = selectEvidence(chunks);
        // 再执行总预算保护，正常情况下 Top5 内容完整保留。
        BudgetResult budgetResult = applyBudget(selection.selected());
        String promptContext = renderer.render(budgetResult.evidences(), properties.isInsufficientEvidenceMarkerEnabled());
        return RagEvidenceAssemblyResult.builder()
                .promptContext(promptContext)
                .promptEvidences(budgetResult.evidences())
                .filteredEvidences(mergeFiltered(selection.filtered(), budgetResult.filtered()))
                .budgetProtectionTriggered(budgetResult.triggered())
                .originalChunkCount(chunks.size())
                .promptEvidenceCount(budgetResult.evidences().size())
                .promptContextChars(promptContext.length())
                .build();
    }

    /**
     * selectEvidence（选择证据）用于完成去重、空内容过滤和单文档数量限制。
     *
     * @param chunks Rerank（重排序）后的切片列表
     * @return 选择结果
     */
    private EvidenceSelection selectEvidence(List<KnowledgeRetrievedChunk> chunks) {
        // 使用有序集合记录已经出现过的切片编号。
        Set<String> seenChunkIds = new LinkedHashSet<>();
        Map<String, Integer> documentCounts = new HashMap<>();
        List<RagEvidence> selected = new ArrayList<>();
        List<RagEvidence> filtered = new ArrayList<>();
        for (KnowledgeRetrievedChunk chunk : chunks) {
            RagEvidence evidence = toEvidence(chunk, selected.size() + 1);
            if (isBlank(evidence.getContent()) || evidence.getContent().trim().length() < Math.max(properties.getMinContentChars(), 1)) {
                evidence.setFilteredReason("内容为空或过短");
                filtered.add(evidence);
                continue;
            }
            if (!isBlank(evidence.getChunkId()) && !seenChunkIds.add(evidence.getChunkId())) {
                evidence.setFilteredReason("重复切片");
                filtered.add(evidence);
                continue;
            }
            String documentKey = isBlank(evidence.getDocumentId()) ? "__unknown_document__" + selected.size() : evidence.getDocumentId();
            int documentCount = documentCounts.getOrDefault(documentKey, 0);
            if (documentCount >= Math.max(properties.getMaxEvidencePerDocument(), 1)) {
                evidence.setFilteredReason("同一文档证据过多");
                filtered.add(evidence);
                continue;
            }
            documentCounts.put(documentKey, documentCount + 1);
            selected.add(evidence);
            if (selected.size() >= Math.max(properties.getMaxEvidenceCount(), 1)) {
                break;
            }
        }
        // 重新整理最终证据序号。
        reindex(selected);
        return new EvidenceSelection(selected, filtered);
    }

    /**
     * applyBudget（应用预算）用于在 Top5 总长度过长时优先减少证据条数，最后才截断单条证据。
     *
     * @param evidences 初选证据
     * @return 预算处理结果
     */
    private BudgetResult applyBudget(List<RagEvidence> evidences) {
        // 为空时无需预算处理。
        if (evidences == null || evidences.isEmpty()) {
            return new BudgetResult(List.of(), List.of(), false);
        }
        List<RagEvidence> kept = new ArrayList<>(evidences);
        List<RagEvidence> filtered = new ArrayList<>();
        int maxChars = Math.max(properties.getMaxPromptContextChars(), 1);
        boolean triggered = false;
        // 正常情况下完整保留；只有超出总预算时才逐条移除尾部低优先级证据。
        while (renderLength(kept) > maxChars && kept.size() > 1) {
            RagEvidence removed = kept.remove(kept.size() - 1);
            removed.setFilteredReason("证据总长度超预算，优先移除低排序证据");
            filtered.add(removed);
            triggered = true;
        }
        // 只剩一条仍超预算时，按配置截断单条内容。
        if (renderLength(kept) > maxChars && properties.isTruncateSingleEvidenceWhenNecessary() && !kept.isEmpty()) {
            RagEvidence evidence = kept.get(0);
            int allowedContentChars = Math.max(maxChars - 500, 200);
            if (evidence.getContent() != null && evidence.getContent().length() > allowedContentChars) {
                evidence.setContent(evidence.getContent().substring(0, allowedContentChars));
                evidence.setTruncated(true);
                triggered = true;
            }
        }
        reindex(kept);
        return new BudgetResult(kept, filtered, triggered);
    }

    /**
     * toEvidence（转换证据）用于把检索切片映射为 RAG 证据对象。
     *
     * @param chunk 检索切片
     * @param evidenceIndex 证据序号
     * @return RAG 证据
     */
    private RagEvidence toEvidence(KnowledgeRetrievedChunk chunk, int evidenceIndex) {
        // 兼容空切片，避免异常影响主链路。
        if (chunk == null) {
            return RagEvidence.builder().evidenceIndex(evidenceIndex).filteredReason("空切片").build();
        }
        return RagEvidence.builder()
                .evidenceIndex(evidenceIndex)
                .knowledgeBaseId(chunk.getKnowledgeBaseId())
                .documentId(chunk.getDocumentId())
                .chunkId(chunk.getChunkId())
                .chunkIndex(chunk.getChunkIndex())
                .titlePath(chunk.getTitlePath())
                .source(chunk.getSource())
                .finalScore(resolveFinalScore(chunk))
                .vectorScore(chunk.getVectorScore())
                .keywordScore(chunk.getKeywordScore())
                .rrfScore(chunk.getRrfScore())
                .rerankScore(chunk.getRerankScore())
                .content(chunk.getContent())
                .truncated(false)
                .filteredReason(null)
                .build();
    }

    /**
     * resolveFinalScore（解析最终分数）用于优先使用 Rerank（重排序）分数。
     *
     * @param chunk 检索切片
     * @return 最终分数
     */
    private Double resolveFinalScore(KnowledgeRetrievedChunk chunk) {
        // 按 Rerank、score、RRF、向量、关键词的顺序兜底。
        if (chunk.getRerankScore() != null) {
            return chunk.getRerankScore();
        }
        if (chunk.getScore() != null) {
            return chunk.getScore();
        }
        if (chunk.getRrfScore() != null) {
            return chunk.getRrfScore();
        }
        if (chunk.getVectorScore() != null) {
            return chunk.getVectorScore();
        }
        return chunk.getKeywordScore();
    }

    /**
     * renderLength（渲染长度）用于估算当前证据进入 Prompt（提示词）后的字符数。
     *
     * @param evidences 证据列表
     * @return 渲染后字符数
     */
    private int renderLength(List<RagEvidence> evidences) {
        // 复用渲染器得到最接近真实 Prompt（提示词）的长度。
        return renderer.render(evidences, properties.isInsufficientEvidenceMarkerEnabled()).length();
    }

    /**
     * reindex（重排序号）用于在过滤和预算保护后保证证据序号连续。
     *
     * @param evidences 证据列表
     */
    private void reindex(List<RagEvidence> evidences) {
        // 按当前列表顺序重新编号。
        for (int index = 0; index < evidences.size(); index++) {
            evidences.get(index).setEvidenceIndex(index + 1);
        }
    }

    /**
     * mergeFiltered（合并过滤证据）用于把选择阶段和预算阶段的过滤结果合并。
     *
     * @param first 第一组过滤结果
     * @param second 第二组过滤结果
     * @return 合并后的过滤结果
     */
    private List<RagEvidence> mergeFiltered(List<RagEvidence> first, List<RagEvidence> second) {
        // 合并后按分数倒序排列，方便调试观察。
        List<RagEvidence> merged = new ArrayList<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return merged.stream()
                .sorted(Comparator.comparing(RagEvidence::getFinalScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * isBlank（是否空白）用于判断字符串是否为空。
     *
     * @param value 原始文本
     * @return true 表示空白
     */
    private boolean isBlank(String value) {
        // 空引用或空白字符串都视为空。
        return value == null || value.isBlank();
    }

    /** EvidenceSelection（证据选择结果）用于在内部传递已选和已过滤证据。 */
    private record EvidenceSelection(List<RagEvidence> selected, List<RagEvidence> filtered) {
    }

    /** BudgetResult（预算处理结果）用于在内部传递预算保护后的证据。 */
    private record BudgetResult(List<RagEvidence> evidences, List<RagEvidence> filtered, boolean triggered) {
    }
}
