package com.clipforge.creation.prompt;

import com.clipforge.contract.creation.CreationPlanResult;
import com.clipforge.creation.evaluation.CreationPlanEvaluationResult;
import com.clipforge.creation.evaluation.EvaluationDimensionScore;
import com.clipforge.creation.model.AgentStep;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * CreationPromptContextFormatter（创作提示词上下文格式化器）。
 *
 * <p>该组件只负责创作领域内部对象的文本清洗和格式化，不读取模板、不访问数据库、不调用模型。
 */
@Component
public class CreationPromptContextFormatter {

    /** maxItemLength（单条输入最大长度）用于限制动态输入，避免异常长文本污染 Prompt（提示词）。 */
    private static final int MAX_ITEM_LENGTH = 500;

    /**
     * formatList（格式化列表）用于把字符串列表转换为带稳定编号的 Prompt（提示词）文本。
     *
     * @param items 待格式化列表
     * @return 格式化后的文本
     */
    public String formatList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "- 暂无可用内容";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < items.size(); index++) {
            builder.append("- [").append(index + 1).append("] ");
            builder.append(sanitize(items.get(index)));
            builder.append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    /**
     * formatPlanSteps（格式化计划步骤）用于把 AgentPlan（智能体计划）步骤转换为 Prompt（提示词）文本。
     *
     * @param steps 智能体计划步骤列表
     * @return 格式化后的计划步骤文本
     */
    public String formatPlanSteps(List<AgentStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return "- 暂无计划步骤";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < steps.size(); index++) {
            AgentStep step = steps.get(index);
            builder.append("- 步骤 ").append(index + 1).append("：").append(sanitize(step.getName()));
            builder.append("，").append(sanitize(step.getDescription()));
            builder.append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    /**
     * formatCreationPlanResult（格式化创作方案结果）用于把待评估方案转换为稳定文本。
     *
     * @param result 创作方案结果
     * @return 格式化后的创作方案文本
     */
    public String formatCreationPlanResult(CreationPlanResult result) {
        if (result == null) {
            return "暂无可评估创作方案";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("theme（创作主题）：").append(sanitize(result.getTheme())).append(System.lineSeparator());
        builder.append("targetAudience（目标受众）：").append(sanitize(result.getTargetAudience())).append(System.lineSeparator());
        builder.append("hook（开头钩子）：").append(sanitize(result.getHook())).append(System.lineSeparator());
        builder.append("storyboard（分镜脚本）：").append(System.lineSeparator()).append(formatList(result.getStoryboard())).append(System.lineSeparator());
        builder.append("voiceoverScript（旁白脚本）：").append(sanitizeLongText(result.getVoiceoverScript())).append(System.lineSeparator());
        builder.append("subtitleSuggestions（字幕建议）：").append(System.lineSeparator()).append(formatList(result.getSubtitleSuggestions())).append(System.lineSeparator());
        builder.append("titleCandidates（标题候选）：").append(System.lineSeparator()).append(formatList(result.getTitleCandidates())).append(System.lineSeparator());
        builder.append("publishCopy（发布文案）：").append(sanitizeLongText(result.getPublishCopy())).append(System.lineSeparator());
        builder.append("hashtags（话题标签）：").append(System.lineSeparator()).append(formatList(result.getHashtags()));
        return builder.toString().stripTrailing();
    }

    /**
     * formatEvaluationResult（格式化评估结果）用于把结构化评估结果转换为修正 Prompt（提示词）文本。
     *
     * @param result 创作方案评估结果
     * @return 格式化后的评估结果文本
     */
    public String formatEvaluationResult(CreationPlanEvaluationResult result) {
        if (result == null) {
            return "暂无评估结果";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("overallScore（综合评分）：").append(result.getOverallScore()).append(System.lineSeparator());
        builder.append("revisionRequired（是否需要修正）：").append(result.isRevisionRequired()).append(System.lineSeparator());
        builder.append("dimensionScores（维度评分）：").append(System.lineSeparator());
        if (result.getDimensionScores() != null) {
            for (EvaluationDimensionScore score : result.getDimensionScores()) {
                builder.append("- ")
                        .append(sanitize(score.getDimension()))
                        .append("（")
                        .append(sanitize(score.getDimensionName()))
                        .append("）：")
                        .append(score.getScore())
                        .append("，原因：")
                        .append(sanitize(score.getReason()))
                        .append(System.lineSeparator());
            }
        }
        builder.append("strengths（优点）：").append(System.lineSeparator()).append(formatList(result.getStrengths())).append(System.lineSeparator());
        builder.append("weaknesses（不足）：").append(System.lineSeparator()).append(formatList(result.getWeaknesses())).append(System.lineSeparator());
        builder.append("revisionAdvice（修正建议）：").append(System.lineSeparator()).append(formatList(result.getRevisionAdvice()));
        return builder.toString().stripTrailing();
    }

    /**
     * sanitize（清洗文本）用于降低 Prompt Injection（提示词注入）和异常长输入风险。
     *
     * @param text 原始文本
     * @return 清洗后的文本
     */
    public String sanitize(String text) {
        String safeText = defaultText(text, "");
        safeText = safeText.replace("---", "___");
        if (safeText.length() > MAX_ITEM_LENGTH) {
            return safeText.substring(0, MAX_ITEM_LENGTH) + "...";
        }
        return safeText;
    }

    /**
     * sanitizeLongText（清洗长文本）用于处理模型原始输出，保留更多内容给修复模型参考。
     *
     * @param text 原始文本
     * @return 清洗后的长文本
     */
    public String sanitizeLongText(String text) {
        String safeText = defaultText(text, "");
        safeText = safeText.replace("---", "___");
        if (safeText.length() > MAX_ITEM_LENGTH * 4) {
            return safeText.substring(0, MAX_ITEM_LENGTH * 4) + "...";
        }
        return safeText;
    }

    /**
     * sanitizeCompressedText（清洗压缩文本）用于处理已经被 Prompt Compression（提示词压缩）裁剪过的文本。
     *
     * @param text 压缩后的文本
     * @return 清洗后的压缩文本
     */
    public String sanitizeCompressedText(String text) {
        String safeText = defaultText(text, "");
        return safeText.replace("---", "___");
    }

    /**
     * defaultText（默认文本）用于处理空字符串和空引用。
     *
     * @param text 原始文本
     * @param defaultValue 默认值
     * @return 非空文本
     */
    public String defaultText(String text, String defaultValue) {
        if (text == null || text.isBlank()) {
            return defaultValue;
        }
        return text;
    }
}
