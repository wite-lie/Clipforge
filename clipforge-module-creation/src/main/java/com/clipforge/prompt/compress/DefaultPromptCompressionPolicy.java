package com.clipforge.prompt.compress;

import com.clipforge.creation.model.AgentContext;
import com.clipforge.agent.prompt.MemoryPromptRenderer;
import com.clipforge.material.MaterialSummary;
import com.clipforge.prompt.budget.TokenBudgetDecision;
import com.clipforge.prompt.budget.TokenBudgetSectionBudget;
import com.clipforge.prompt.token.PromptSectionType;
import com.clipforge.prompt.token.PromptTokenEstimateResult;
import com.clipforge.prompt.token.PromptTokenEstimator;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalResult;
import com.clipforge.material.video.understanding.VideoUnderstandingCreationContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DefaultPromptCompressionPolicy（默认提示词压缩策略）。
 *
 * <p>该策略只做本地规则裁剪，不调用 LLM（大语言模型），也不修改数据库中的原始素材、知识和记忆内容。
 * 当前第一版优先裁剪 RAG（检索增强生成）、图片摘要、视频摘要和 Memory（记忆）等动态上下文。
 */
@Component
@RequiredArgsConstructor
public class DefaultPromptCompressionPolicy implements PromptCompressionPolicy {

    /** RAG（检索增强生成）压缩后写入 AgentContext（智能体上下文）的覆盖键。 */
    public static final String COMPRESSED_RAG_CONTEXT = "COMPRESSED_RAG_CONTEXT";

    /** 图片摘要压缩后写入 AgentContext（智能体上下文）的覆盖键。 */
    public static final String COMPRESSED_IMAGE_SUMMARY = "COMPRESSED_IMAGE_SUMMARY";

    /** 视频摘要压缩后写入 AgentContext（智能体上下文）的覆盖键。 */
    public static final String COMPRESSED_VIDEO_SUMMARY = "COMPRESSED_VIDEO_SUMMARY";

    /** Memory（记忆）上下文压缩后写入 AgentContext（智能体上下文）的覆盖键。 */
    public static final String COMPRESSED_MEMORY_CONTEXT = "COMPRESSED_MEMORY_CONTEXT";

    /** 裁剪提示，用于告诉模型当前片段不是完整原文。 */
    private static final String TRUNCATION_MARKER = "\n\n[以上内容已按 Token Budget（词元预算）裁剪，仅保留关键开头内容。]";

    /** properties（配置属性）用于读取压缩开关和每个片段的字符上限。 */
    private final PromptCompressionProperties properties;

    /** promptTokenEstimator（提示词词元估算器）用于估算裁剪前后的 Token（词元）变化。 */
    private final PromptTokenEstimator promptTokenEstimator;

    /** memoryPromptRenderer（记忆提示词渲染器）用于得到和真实 Prompt（提示词）一致的记忆文本。 */
    private final MemoryPromptRenderer memoryPromptRenderer;

    /**
     * compress（压缩）根据 Token Budget（词元预算）决策生成动态上下文覆盖文本。
     *
     * @param request 压缩请求
     * @return 压缩结果
     */
    @Override
    public PromptCompressionResult compress(PromptCompressionRequest request) {
        if (!properties.isEnabled()) {
            return skipped("Prompt Compression（提示词压缩）配置已关闭", request);
        }
        if (request == null || request.getContext() == null) {
            return skipped("缺少 AgentContext（智能体上下文），跳过压缩", request);
        }
        if (!shouldCompress(request.getBudgetDecision())) {
            return skipped("Token Budget（词元预算）未超限，跳过压缩", request);
        }

        Map<String, String> overrides = new LinkedHashMap<>();
        List<PromptSectionCompressionResult> sectionResults = new ArrayList<>();
        AgentContext context = request.getContext();

        compressSection(context, PromptSectionType.RAG_CONTEXT, COMPRESSED_RAG_CONTEXT, readRagContext(context), overrides, sectionResults);
        compressSection(context, PromptSectionType.IMAGE_SUMMARY, COMPRESSED_IMAGE_SUMMARY, readImageSummary(context), overrides, sectionResults);
        compressSection(context, PromptSectionType.VIDEO_SUMMARY, COMPRESSED_VIDEO_SUMMARY, readVideoSummary(context), overrides, sectionResults);
        compressSection(context, PromptSectionType.MEMORY_CONTEXT, COMPRESSED_MEMORY_CONTEXT, readMemoryContext(context), overrides, sectionResults);

        boolean compressed = sectionResults.stream().anyMatch(item -> item.getAction() == PromptCompressionAction.TRUNCATE_TAIL);
        PromptCompressionReport report = PromptCompressionReport.builder()
                .compressed(compressed)
                .reason(compressed ? "存在超预算或超片段限制，已按字符上限裁剪动态上下文" : "触发了压缩检查，但没有可裁剪内容")
                .beforeEstimatedTokens(readTotalTokens(request.getEstimateResult()))
                .afterEstimatedTokens(null)
                .sections(sectionResults)
                .build();
        return PromptCompressionResult.builder()
                .compressed(compressed)
                .overrides(overrides)
                .report(report)
                .build();
    }

    /**
     * shouldCompress（是否压缩）判断预算决策是否要求裁剪。
     *
     * @param decision Token Budget（词元预算）决策
     * @return 是否需要压缩
     */
    private boolean shouldCompress(TokenBudgetDecision decision) {
        if (decision == null || !Boolean.TRUE.equals(decision.getEnabled())) {
            return false;
        }
        if (Boolean.TRUE.equals(decision.getOverBudget())) {
            return true;
        }
        return decision.getViolatedSections() != null && !decision.getViolatedSections().isEmpty();
    }

    /**
     * compressSection（压缩片段）对单个动态上下文片段做裁剪。
     *
     * @param context AgentContext（智能体上下文）
     * @param sectionType 片段类型
     * @param overrideKey 覆盖文本键
     * @param originalText 原始文本
     * @param overrides 覆盖文本集合
     * @param sectionResults 片段压缩结果集合
     */
    private void compressSection(AgentContext context, PromptSectionType sectionType, String overrideKey, String originalText, Map<String, String> overrides, List<PromptSectionCompressionResult> sectionResults) {
        String safeText = originalText == null ? "" : originalText;
        if (safeText.isBlank()) {
            sectionResults.add(buildSectionResult(sectionType, PromptCompressionAction.SKIP_EMPTY, safeText, safeText, "片段为空，无需压缩"));
            return;
        }
        int charLimit = resolveCharLimit(sectionType);
        if (charLimit <= 0) {
            sectionResults.add(buildSectionResult(sectionType, PromptCompressionAction.KEEP, safeText, safeText, "未配置有效字符上限，保持原文"));
            return;
        }
        if (safeText.length() <= charLimit) {
            sectionResults.add(buildSectionResult(sectionType, PromptCompressionAction.KEEP, safeText, safeText, "未超过字符上限，保持原文"));
            return;
        }

        String compressedText = safeText.substring(0, charLimit).stripTrailing();
        if (properties.isAppendTruncationMarker()) {
            compressedText = compressedText + TRUNCATION_MARKER;
        }
        overrides.put(overrideKey, compressedText);
        context.putArtifact(overrideKey, compressedText);
        sectionResults.add(buildSectionResult(sectionType, PromptCompressionAction.TRUNCATE_TAIL, safeText, compressedText, "超过片段字符上限，保留开头并裁剪尾部"));
    }

    /**
     * buildSectionResult（构建片段结果）生成单个片段的压缩报告。
     *
     * @param sectionType 片段类型
     * @param action 压缩动作
     * @param beforeText 压缩前文本
     * @param afterText 压缩后文本
     * @param reason 原因
     * @return 片段压缩结果
     */
    private PromptSectionCompressionResult buildSectionResult(PromptSectionType sectionType, PromptCompressionAction action, String beforeText, String afterText, String reason) {
        String safeBeforeText = beforeText == null ? "" : beforeText;
        String safeAfterText = afterText == null ? "" : afterText;
        return PromptSectionCompressionResult.builder()
                .sectionType(sectionType)
                .action(action)
                .beforeCharacters(safeBeforeText.length())
                .afterCharacters(safeAfterText.length())
                .beforeEstimatedTokens(promptTokenEstimator.estimateText(safeBeforeText))
                .afterEstimatedTokens(promptTokenEstimator.estimateText(safeAfterText))
                .reason(reason)
                .build();
    }

    /**
     * resolveCharLimit（解析字符上限）按片段类型读取配置。
     *
     * @param sectionType 片段类型
     * @return 字符上限
     */
    private int resolveCharLimit(PromptSectionType sectionType) {
        if (sectionType == null || properties.getSectionCharLimits() == null) {
            return 0;
        }
        String enumKey = sectionType.value();
        Integer direct = properties.getSectionCharLimits().get(enumKey);
        if (direct != null) {
            return direct;
        }
        Integer kebab = properties.getSectionCharLimits().get(enumKey.toLowerCase(Locale.ROOT).replace("_", "-"));
        return kebab == null ? 0 : kebab;
    }

    /**
     * readRagContext（读取检索上下文）从 AgentContext（智能体上下文）读取 RAG（检索增强生成）注入文本。
     *
     * @param context AgentContext（智能体上下文）
     * @return 检索上下文文本
     */
    private String readRagContext(AgentContext context) {
        Object value = context.getArtifact("KNOWLEDGE_RETRIEVAL_RESULT");
        if (value instanceof KnowledgeRetrievalResult result) {
            return result.getPromptContext();
        }
        return "";
    }

    /**
     * readImageSummary（读取图片摘要）从 AgentContext（智能体上下文）读取图片理解摘要。
     *
     * @param context AgentContext（智能体上下文）
     * @return 图片摘要文本
     */
    private String readImageSummary(AgentContext context) {
        Object value = context.getArtifact("MATERIAL_SUMMARY");
        if (value instanceof MaterialSummary summary) {
            return summary.getSummaryText();
        }
        return "";
    }

    /**
     * readVideoSummary（读取视频摘要）从 AgentContext（智能体上下文）读取视频理解摘要。
     *
     * @param context AgentContext（智能体上下文）
     * @return 视频摘要文本
     */
    private String readVideoSummary(AgentContext context) {
        Object value = context.getArtifact("VIDEO_UNDERSTANDING_CREATION_CONTEXT");
        if (value instanceof VideoUnderstandingCreationContext videoContext) {
            return videoContext.getSummaryText();
        }
        return "";
    }

    /**
     * readMemoryContext（读取记忆上下文）把 MemoryContext（记忆上下文）渲染为可注入 Prompt（提示词）的文本。
     *
     * @param context AgentContext（智能体上下文）
     * @return 记忆上下文文本
     */
    private String readMemoryContext(AgentContext context) {
        return memoryPromptRenderer.render(context.getMemoryContext());
    }

    /**
     * skipped（跳过压缩）构建未压缩结果。
     *
     * @param reason 跳过原因
     * @param request 压缩请求
     * @return 未压缩结果
     */
    private PromptCompressionResult skipped(String reason, PromptCompressionRequest request) {
        return PromptCompressionResult.builder()
                .compressed(false)
                .overrides(Map.of())
                .report(PromptCompressionReport.builder()
                        .compressed(false)
                        .reason(reason)
                        .beforeEstimatedTokens(request == null ? 0 : readTotalTokens(request.getEstimateResult()))
                        .afterEstimatedTokens(request == null ? 0 : readTotalTokens(request.getEstimateResult()))
                        .sections(List.of())
                        .build())
                .build();
    }

    /**
     * readTotalTokens（读取总词元）安全读取估算结果中的总 Token（词元）。
     *
     * @param estimateResult 估算结果
     * @return 总 Token（词元）
     */
    private int readTotalTokens(PromptTokenEstimateResult estimateResult) {
        return estimateResult == null || estimateResult.getTotalEstimatedTokens() == null ? 0 : estimateResult.getTotalEstimatedTokens();
    }
}
