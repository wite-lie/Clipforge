package com.clipforge.prompt.budget;

import com.clipforge.prompt.token.PromptSectionType;
import com.clipforge.prompt.token.PromptTokenEstimateResult;
import com.clipforge.prompt.token.PromptTokenRiskLevel;
import com.clipforge.prompt.token.PromptTokenSectionEstimate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DefaultTokenBudgetPolicy（默认词元预算策略）。
 *
 * <p>该策略根据配置中的 TokenBudgetProfile（词元预算档案）判断总输入预算和各来源片段预算是否超限。
 */
@Component
@RequiredArgsConstructor
public class DefaultTokenBudgetPolicy implements TokenBudgetPolicy {

    /** properties（词元预算配置）用于读取默认预算和调用用途预算档案。 */
    private final TokenBudgetProperties properties;

    /**
     * decide（决策）用于生成模型调用前的预算判断。
     *
     * @param purpose 模型调用用途
     * @param estimateResult 提示词词元估算结果
     * @return 词元预算决策
     */
    @Override
    public TokenBudgetDecision decide(String purpose, PromptTokenEstimateResult estimateResult) {
        // 解析安全用途。
        String safePurpose = purpose == null || purpose.isBlank() ? "UNKNOWN" : purpose;
        // 读取预算档案。
        TokenBudgetProfile profile = resolveProfile(safePurpose);
        // 计算上下文窗口。
        int contextWindow = positiveOrDefault(profile.getContextWindow(), properties.getDefaultContextWindow());
        // 计算预留输出 Token（词元）。
        int reservedOutputTokens = positiveOrDefault(profile.getReservedOutputTokens(), properties.getDefaultReservedOutputTokens());
        // 计算安全余量 Token（词元）。
        int safetyTokens = positiveOrDefault(profile.getSafetyTokens(), properties.getDefaultSafetyTokens());
        // 计算输入预算，至少保留 0，避免配置错误导致负数。
        int inputBudgetTokens = Math.max(0, contextWindow - reservedOutputTokens - safetyTokens);
        // 读取估算输入 Token（词元）。
        int estimatedInputTokens = estimateResult == null || estimateResult.getTotalEstimatedTokens() == null ? 0 : estimateResult.getTotalEstimatedTokens();
        // 判断是否超过总输入预算。
        boolean overBudget = estimatedInputTokens > inputBudgetTokens;
        // 构建片段预算明细。
        List<TokenBudgetSectionBudget> sectionBudgets = buildSectionBudgets(estimateResult, profile.getSectionLimits());
        // 读取局部超预算片段。
        List<TokenBudgetSectionBudget> violatedSections = sectionBudgets.stream()
                // 只保留超过局部限制的片段。
                .filter(section -> Boolean.TRUE.equals(section.getOverLimit()))
                // 按超出数量从高到低排序。
                .sorted(Comparator.comparing(TokenBudgetSectionBudget::getOverLimitTokens).reversed())
                // 收集为列表。
                .toList();
        // 构建决策。
        return TokenBudgetDecision.builder()
                // 设置是否启用。
                .enabled(properties.isEnabled())
                // 设置用途。
                .purpose(safePurpose)
                // 设置档案名称。
                .profileName(normalizePurpose(safePurpose))
                // 设置上下文窗口。
                .modelContextWindow(contextWindow)
                // 设置预留输出。
                .reservedOutputTokens(reservedOutputTokens)
                // 设置安全余量。
                .reservedSafetyTokens(safetyTokens)
                // 设置输入预算。
                .inputBudgetTokens(inputBudgetTokens)
                // 设置估算输入。
                .estimatedInputTokens(estimatedInputTokens)
                // 设置是否超总预算。
                .overBudget(overBudget)
                // 设置超出数量。
                .overBudgetTokens(Math.max(0, estimatedInputTokens - inputBudgetTokens))
                // 设置风险等级。
                .riskLevel(resolveRiskLevel(overBudget, violatedSections, estimateResult))
                // 设置片段预算明细。
                .sectionBudgets(sectionBudgets)
                // 设置超限片段。
                .violatedSections(violatedSections)
                // 设置建议动作。
                .recommendedActions(buildRecommendedActions(overBudget, violatedSections, estimateResult))
                // 构建决策。
                .build();
    }

    /**
     * resolveProfile（解析档案）用于按调用用途读取预算配置，没有配置时使用空档案兜底。
     *
     * @param purpose 调用用途
     * @return 预算档案
     */
    private TokenBudgetProfile resolveProfile(String purpose) {
        // 读取配置中的档案 Map（映射）。
        Map<String, TokenBudgetProfile> profiles = properties.getProfiles();
        // 如果没有配置档案，则返回空档案。
        if (profiles == null || profiles.isEmpty()) {
            // 返回空档案。
            return new TokenBudgetProfile();
        }
        // 根据用途规范化为配置键。
        String profileKey = normalizePurpose(purpose);
        // 返回命中的档案，未命中时返回空档案。
        return profiles.getOrDefault(profileKey, new TokenBudgetProfile());
    }

    /**
     * buildSectionBudgets（构建片段预算）用于判断每个估算片段是否超过局部限制。
     *
     * @param estimateResult 估算结果
     * @param sectionLimits 片段限制配置
     * @return 片段预算明细
     */
    private List<TokenBudgetSectionBudget> buildSectionBudgets(PromptTokenEstimateResult estimateResult, Map<String, Integer> sectionLimits) {
        // 估算结果为空时返回空列表。
        if (estimateResult == null || estimateResult.getSections() == null) {
            // 返回空列表。
            return List.of();
        }
        // 创建预算明细列表。
        List<TokenBudgetSectionBudget> budgets = new ArrayList<>();
        // 遍历估算片段。
        for (PromptTokenSectionEstimate section : estimateResult.getSections()) {
            // 读取局部限制。
            Integer limit = resolveSectionLimit(section.getType(), sectionLimits);
            // 读取估算 Token（词元）。
            int estimatedTokens = section.getEstimatedTokens() == null ? 0 : section.getEstimatedTokens();
            // 判断是否超限。
            boolean overLimit = limit != null && estimatedTokens > limit;
            // 添加片段预算明细。
            budgets.add(TokenBudgetSectionBudget.builder()
                    // 设置片段类型。
                    .sectionType(section.getType())
                    // 设置片段名称。
                    .sectionName(section.getName())
                    // 设置估算 Token（词元）。
                    .estimatedTokens(estimatedTokens)
                    // 设置局部限制。
                    .limitTokens(limit)
                    // 设置是否超限。
                    .overLimit(overLimit)
                    // 设置超出数量。
                    .overLimitTokens(limit == null ? 0 : Math.max(0, estimatedTokens - limit))
                    // 设置状态。
                    .status(limit == null ? TokenBudgetSectionStatus.NO_LIMIT : overLimit ? TokenBudgetSectionStatus.OVER_LIMIT : TokenBudgetSectionStatus.OK)
                    // 设置是否计入总数。
                    .includedInTotal(section.getIncludedInTotal())
                    // 设置占比。
                    .ratio(section.getRatio())
                    // 构建明细。
                    .build());
        }
        // 返回预算明细。
        return budgets;
    }

    /**
     * resolveSectionLimit（解析片段限制）用于按枚举类型读取配置中的局部预算。
     *
     * @param type 片段类型
     * @param sectionLimits 片段限制配置
     * @return 局部预算；为空表示没有限制
     */
    private Integer resolveSectionLimit(PromptSectionType type, Map<String, Integer> sectionLimits) {
        // 没有配置时返回空。
        if (type == null || sectionLimits == null || sectionLimits.isEmpty()) {
            // 返回空。
            return null;
        }
        // 支持枚举名和 kebab-case（短横线命名）两种配置键。
        Integer direct = sectionLimits.get(type.value());
        // 命中直接键时返回。
        if (direct != null) {
            // 返回直接配置。
            return direct;
        }
        // 返回 kebab-case 配置。
        return sectionLimits.get(toKebabCase(type.value()));
    }

    /**
     * resolveRiskLevel（解析风险等级）用于综合总预算和局部预算状态。
     *
     * @param overBudget 是否超过总预算
     * @param violatedSections 超限片段
     * @param estimateResult 估算结果
     * @return 风险等级
     */
    private PromptTokenRiskLevel resolveRiskLevel(boolean overBudget, List<TokenBudgetSectionBudget> violatedSections, PromptTokenEstimateResult estimateResult) {
        // 总预算超限时直接高风险。
        if (overBudget) {
            // 返回高风险。
            return PromptTokenRiskLevel.HIGH;
        }
        // 局部片段超限时至少中风险。
        if (violatedSections != null && !violatedSections.isEmpty()) {
            // 返回中风险。
            return PromptTokenRiskLevel.MEDIUM;
        }
        // 没有估算结果时低风险。
        if (estimateResult == null || estimateResult.getRiskLevel() == null) {
            // 返回低风险。
            return PromptTokenRiskLevel.LOW;
        }
        // 沿用估算器风险等级。
        return estimateResult.getRiskLevel();
    }

    /**
     * buildRecommendedActions（构建建议动作）用于输出后续优化项 8 的裁剪提示。
     *
     * @param overBudget 是否超过总预算
     * @param violatedSections 超限片段
     * @param estimateResult 估算结果
     * @return 建议动作列表
     */
    private List<String> buildRecommendedActions(boolean overBudget, List<TokenBudgetSectionBudget> violatedSections, PromptTokenEstimateResult estimateResult) {
        // 创建建议列表。
        List<String> actions = new ArrayList<>();
        // 总预算超限时给出总量提示。
        if (overBudget) {
            // 添加总预算建议。
            actions.add("估算输入已经超过总输入预算，优化项 8 应优先裁剪 RAG（检索增强生成）、图片摘要、视频摘要和会话历史。");
        }
        // 遍历局部超限片段。
        if (violatedSections != null) {
            // 为每个超限片段添加建议。
            for (TokenBudgetSectionBudget section : violatedSections) {
                // 添加局部预算建议。
                actions.add("片段 " + section.getSectionType() + " 超出局部预算 " + section.getOverLimitTokens() + " Token（词元），后续应优先裁剪或压缩。");
            }
        }
        // 如果没有明确超限，则使用估算器建议来源。
        if (actions.isEmpty() && estimateResult != null && estimateResult.getRecommendedTrimSectionTypes() != null && !estimateResult.getRecommendedTrimSectionTypes().isEmpty()) {
            // 添加预防性建议。
            actions.add("当前未超预算，后续如需压缩可优先关注：" + estimateResult.getRecommendedTrimSectionTypes());
        }
        // 没有任何建议时给出保持提示。
        if (actions.isEmpty()) {
            // 添加保持当前策略建议。
            actions.add("当前 Prompt（提示词）预算状态正常，本阶段不需要裁剪。");
        }
        // 返回建议列表。
        return actions;
    }

    /**
     * positiveOrDefault（正数或默认值）用于处理空配置和非正配置。
     *
     * @param value 配置值
     * @param defaultValue 默认值
     * @return 可用正数
     */
    private int positiveOrDefault(Integer value, Integer defaultValue) {
        // 配置值存在且大于 0 时使用配置值。
        if (value != null && value > 0) {
            // 返回配置值。
            return value;
        }
        // 默认值存在且大于 0 时使用默认值。
        if (defaultValue != null && defaultValue > 0) {
            // 返回默认值。
            return defaultValue;
        }
        // 兜底返回 0。
        return 0;
    }

    /**
     * normalizePurpose（规范化用途）用于把 CREATION_PLAN_GENERATION 转成 creation-plan-generation。
     *
     * @param purpose 原始用途
     * @return 配置键
     */
    private String normalizePurpose(String purpose) {
        // 转成小写并把下划线替换成短横线。
        return purpose.toLowerCase(Locale.ROOT).replace("_", "-");
    }

    /**
     * toKebabCase（转短横线命名）用于把枚举名称转成配置键。
     *
     * @param value 原始枚举名
     * @return 短横线命名
     */
    private String toKebabCase(String value) {
        // 转成小写并把下划线替换为短横线。
        return value.toLowerCase(Locale.ROOT).replace("_", "-");
    }
}
