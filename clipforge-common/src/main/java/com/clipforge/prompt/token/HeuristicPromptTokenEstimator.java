package com.clipforge.prompt.token;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * HeuristicPromptTokenEstimator（启发式提示词词元估算器）。
 *
 * <p>该实现不接入真实 tokenizer（分词器），只用本地规则粗略估算 Token（词元）数量，适合成本观察和上下文风险预警。
 */
@Component
public class HeuristicPromptTokenEstimator implements PromptTokenEstimator {

    /** WARNING_THRESHOLD（警戒阈值）表示超过该 Token（词元）数量后进入中风险。 */
    private static final int WARNING_THRESHOLD = 8_000;

    /** HIGH_RISK_THRESHOLD（高风险阈值）表示超过该 Token（词元）数量后进入高风险。 */
    private static final int HIGH_RISK_THRESHOLD = 12_000;

    /**
     * estimateText（估算文本）用于按中文字符、英文词、数字和结构字符粗略估算 Token（词元）。
     *
     * @param text 文本
     * @return 估算 Token（词元）数量
     */
    @Override
    public int estimateText(String text) {
        // 空文本不消耗 Token（词元）。
        if (text == null || text.isBlank()) {
            // 返回 0。
            return 0;
        }
        // 中文字符数量。
        int cjkCount = 0;
        // 英文和数字连续片段数量。
        int latinWordCount = 0;
        // 标点、换行、Markdown（标记文档）和 JSON（对象表示格式）结构字符数量。
        int structureCount = 0;
        // 标记上一字符是否处于英文或数字片段中。
        boolean inLatinWord = false;
        // 遍历文本中的每个字符。
        for (int index = 0; index < text.length(); index++) {
            // 读取当前字符。
            char current = text.charAt(index);
            // 如果是中文、日文或韩文统一表意字符，则按一个字符约一个 Token（词元）估算。
            if (isCjk(current)) {
                // 中文字符计数加一。
                cjkCount++;
                // 结束英文片段。
                inLatinWord = false;
                // 继续下一个字符。
                continue;
            }
            // 如果是英文或数字，则按连续片段估算。
            if (Character.isLetterOrDigit(current)) {
                // 如果当前不在英文片段中，说明出现一个新英文词或数字片段。
                if (!inLatinWord) {
                    // 英文词数量加一。
                    latinWordCount++;
                    // 标记进入英文片段。
                    inLatinWord = true;
                }
                // 继续下一个字符。
                continue;
            }
            // 非英文数字字符会结束英文片段。
            inLatinWord = false;
            // 非空白字符和换行都算作结构负担。
            if (!Character.isWhitespace(current) || current == '\n' || current == '\r') {
                // 结构字符计数加一。
                structureCount++;
            }
        }
        // 中文 Token（词元）近似值。
        int cjkTokens = cjkCount;
        // 英文 Token（词元）近似值，短词按 1 个 Token（词元）计。
        int latinTokens = latinWordCount;
        // 结构 Token（词元）近似值，每 4 个结构字符约 1 个 Token（词元）。
        int structureTokens = (int) Math.ceil(structureCount / 4.0);
        // 长文本附加少量格式损耗，覆盖 Markdown（标记文档）、JSON（对象表示格式）和列表符号带来的误差。
        int overheadTokens = (int) Math.ceil(text.length() / 120.0);
        // 返回至少为 1 的估算结果。
        return Math.max(1, cjkTokens + latinTokens + structureTokens + overheadTokens);
    }

    /**
     * estimate（估算）用于按片段汇总 Prompt（提示词）总 Token（词元）和诊断明细。
     *
     * @param sections 提示词片段列表
     * @return 提示词词元估算结果
     */
    @Override
    public PromptTokenEstimateResult estimate(List<PromptSection> sections) {
        // 空片段列表返回低风险空结果。
        if (sections == null || sections.isEmpty()) {
            // 构建空结果。
            return PromptTokenEstimateResult.builder()
                    // 总 Token 为 0。
                    .totalEstimatedTokens(0)
                    // 总字符为 0。
                    .totalCharacters(0)
                    // 风险等级为低。
                    .riskLevel(PromptTokenRiskLevel.LOW)
                    // 设置警戒阈值。
                    .warningThreshold(WARNING_THRESHOLD)
                    // 设置高风险阈值。
                    .highRiskThreshold(HIGH_RISK_THRESHOLD)
                    // 片段为空。
                    .sections(List.of())
                    // 建议裁剪为空。
                    .recommendedTrimSectionTypes(List.of())
                    // 构建结果。
                    .build();
        }
        // 先计算每个片段的估算值。
        List<PromptTokenSectionEstimate> estimates = sections.stream()
                // 转换为片段估算结果。
                .map(this::estimateSection)
                // 收集为列表。
                .toList();
        // 计算真实计入总数的 Token（词元）。
        int totalTokens = estimates.stream()
                // 只统计真实发送给模型的片段。
                .filter(item -> Boolean.TRUE.equals(item.getIncludedInTotal()))
                // 读取估算 Token（词元）。
                .mapToInt(PromptTokenSectionEstimate::getEstimatedTokens)
                // 求和。
                .sum();
        // 计算真实计入总数的字符数。
        int totalCharacters = estimates.stream()
                // 只统计真实发送给模型的片段。
                .filter(item -> Boolean.TRUE.equals(item.getIncludedInTotal()))
                // 读取字符数。
                .mapToInt(PromptTokenSectionEstimate::getCharacterCount)
                // 求和。
                .sum();
        // 根据总 Token（词元）补充占比。
        List<PromptTokenSectionEstimate> estimatesWithRatio = estimates.stream()
                // 为每个片段计算占比。
                .map(item -> withRatio(item, totalTokens))
                // 收集为列表。
                .toList();
        // 查找真实计入总数的最大片段。
        PromptTokenSectionEstimate largestIncludedSection = estimatesWithRatio.stream()
                // 只看计入总数的片段。
                .filter(item -> Boolean.TRUE.equals(item.getIncludedInTotal()))
                // 按估算 Token（词元）倒序。
                .max(Comparator.comparing(PromptTokenSectionEstimate::getEstimatedTokens))
                // 没有时返回空。
                .orElse(null);
        // 构建最终结果。
        return PromptTokenEstimateResult.builder()
                // 设置总估算 Token（词元）。
                .totalEstimatedTokens(totalTokens)
                // 设置总字符数。
                .totalCharacters(totalCharacters)
                // 设置风险等级。
                .riskLevel(resolveRiskLevel(totalTokens))
                // 设置警戒阈值。
                .warningThreshold(WARNING_THRESHOLD)
                // 设置高风险阈值。
                .highRiskThreshold(HIGH_RISK_THRESHOLD)
                // 设置最大计入片段类型。
                .largestSectionType(largestIncludedSection == null ? null : largestIncludedSection.getType())
                // 设置最大计入片段名称。
                .largestSectionName(largestIncludedSection == null ? null : largestIncludedSection.getName())
                // 设置建议裁剪来源。
                .recommendedTrimSectionTypes(recommendTrimSections(estimatesWithRatio))
                // 设置片段明细。
                .sections(estimatesWithRatio)
                // 构建结果。
                .build();
    }

    /**
     * estimateSection（估算片段）用于把提示词片段转换为估算明细。
     *
     * @param section 提示词片段
     * @return 片段估算结果
     */
    private PromptTokenSectionEstimate estimateSection(PromptSection section) {
        // 读取安全文本。
        String content = section.getContent() == null ? "" : section.getContent();
        // 构建片段估算结果。
        return PromptTokenSectionEstimate.builder()
                // 设置片段类型。
                .type(section.getType())
                // 设置片段名称。
                .name(section.getName())
                // 设置字符数。
                .characterCount(content.length())
                // 设置估算 Token（词元）。
                .estimatedTokens(estimateText(content))
                // 占比稍后根据总数补充。
                .ratio(BigDecimal.ZERO)
                // 设置是否计入总数。
                .includedInTotal(section.isIncludedInTotal())
                // 构建结果。
                .build();
    }

    /**
     * withRatio（补充占比）用于根据真实总 Token（词元）计算片段占比。
     *
     * @param estimate 原片段估算
     * @param totalTokens 总估算 Token（词元）
     * @return 补充占比后的片段估算
     */
    private PromptTokenSectionEstimate withRatio(PromptTokenSectionEstimate estimate, int totalTokens) {
        // 总数为 0 时占比为 0。
        BigDecimal ratio = totalTokens <= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(estimate.getEstimatedTokens())
                // 除以总 Token（词元）。
                .divide(BigDecimal.valueOf(totalTokens), 4, RoundingMode.HALF_UP);
        // 构建新对象，避免修改原对象。
        return PromptTokenSectionEstimate.builder()
                // 保留类型。
                .type(estimate.getType())
                // 保留名称。
                .name(estimate.getName())
                // 保留字符数。
                .characterCount(estimate.getCharacterCount())
                // 保留估算 Token（词元）。
                .estimatedTokens(estimate.getEstimatedTokens())
                // 设置占比。
                .ratio(ratio)
                // 保留是否计入总数。
                .includedInTotal(estimate.getIncludedInTotal())
                // 构建结果。
                .build();
    }

    /**
     * resolveRiskLevel（解析风险等级）用于根据总估算 Token（词元）判断风险。
     *
     * @param totalTokens 总估算 Token（词元）
     * @return 风险等级
     */
    private PromptTokenRiskLevel resolveRiskLevel(int totalTokens) {
        // 超过高风险阈值时返回高风险。
        if (totalTokens >= HIGH_RISK_THRESHOLD) {
            // 返回高风险。
            return PromptTokenRiskLevel.HIGH;
        }
        // 超过警戒阈值时返回中风险。
        if (totalTokens >= WARNING_THRESHOLD) {
            // 返回中风险。
            return PromptTokenRiskLevel.MEDIUM;
        }
        // 其他情况返回低风险。
        return PromptTokenRiskLevel.LOW;
    }

    /**
     * recommendTrimSections（推荐裁剪片段）用于找出后续最值得裁剪的诊断片段。
     *
     * @param estimates 估算明细
     * @return 建议裁剪片段类型列表
     */
    private List<PromptSectionType> recommendTrimSections(List<PromptTokenSectionEstimate> estimates) {
        // 诊断片段中优先关注 RAG、图片、视频和记忆。
        return estimates.stream()
                // 只看诊断片段，避免推荐直接裁剪完整 userPrompt（用户提示词）。
                .filter(item -> !Boolean.TRUE.equals(item.getIncludedInTotal()))
                // 只保留可裁剪来源。
                .filter(item -> isTrimCandidate(item.getType()))
                // 按估算 Token（词元）从高到低排序。
                .sorted(Comparator.comparing(PromptTokenSectionEstimate::getEstimatedTokens).reversed())
                // 最多给出三个建议。
                .limit(3)
                // 读取片段类型。
                .map(PromptTokenSectionEstimate::getType)
                // 收集为列表。
                .toList();
    }

    /**
     * isTrimCandidate（是否裁剪候选）用于判断片段是否适合作为后续预算裁剪对象。
     *
     * @param type 片段类型
     * @return 是否裁剪候选
     */
    private boolean isTrimCandidate(PromptSectionType type) {
        // RAG、Memory、图片摘要、视频摘要和会话历史都可以在后续策略中裁剪。
        return type == PromptSectionType.RAG_CONTEXT
                || type == PromptSectionType.MEMORY_CONTEXT
                || type == PromptSectionType.IMAGE_SUMMARY
                || type == PromptSectionType.VIDEO_SUMMARY
                || type == PromptSectionType.CONVERSATION_HISTORY;
    }

    /**
     * isCjk（是否中日韩字符）用于判断字符是否属于 CJK（中日韩统一表意文字）范围。
     *
     * @param current 当前字符
     * @return 是否 CJK 字符
     */
    private boolean isCjk(char current) {
        // CJK Unified Ideographs（中日韩统一表意文字）基础区。
        return current >= '\u4E00' && current <= '\u9FFF';
    }
}
