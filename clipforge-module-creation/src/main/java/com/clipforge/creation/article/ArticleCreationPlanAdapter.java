package com.clipforge.creation.article;

import com.clipforge.contract.creation.ArticleCreationPlan;
import com.clipforge.contract.creation.ArticleSection;
import com.clipforge.contract.creation.CreationPlanResult;
import java.util.List;

/**
 * ArticleCreationPlanAdapter（图文方案适配器）。
 *
 * <p>该组件把真实图文方案转换为旧接口兼容的 CreationPlanResult（创作方案结果）。
 */
public class ArticleCreationPlanAdapter {

    /**
     * toLegacyResult（转旧结果）用于兼容现有评估、质量记录和旧前端字段。
     *
     * @param articlePlan 图文方案
     * @return 旧创作方案结果
     */
    public CreationPlanResult toLegacyResult(ArticleCreationPlan articlePlan) {
        if (articlePlan == null) {
            return CreationPlanResult.builder()
                    .theme("图文创作方案")
                    .targetAudience("未提供目标受众")
                    .hook("暂无导语")
                    .storyboard(List.of("暂无正文结构"))
                    .voiceoverScript("")
                    .subtitleSuggestions(List.of())
                    .titleCandidates(List.of("图文创作方案"))
                    .publishCopy("")
                    .hashtags(List.of())
                    .score(0)
                    .improvementSuggestions(List.of("图文方案为空，请重新生成。"))
                    .build();
        }
        return CreationPlanResult.builder()
                .theme(articlePlan.getTheme())
                .targetAudience(articlePlan.getTargetAudience())
                .hook(articlePlan.getLead())
                .storyboard(toStoryboard(articlePlan.getSections()))
                .voiceoverScript(toArticleText(articlePlan))
                .subtitleSuggestions(articlePlan.getRiskWarnings() == null ? List.of() : articlePlan.getRiskWarnings())
                .titleCandidates(articlePlan.getTitleCandidates())
                .publishCopy(articlePlan.getPublishCopy())
                .hashtags(articlePlan.getHashtags())
                .score(articlePlan.getScore())
                .improvementSuggestions(articlePlan.getImprovementSuggestions())
                .build();
    }

    private List<String> toStoryboard(List<ArticleSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return List.of("暂无正文结构");
        }
        return sections.stream()
                .map(section -> safe(section.getSectionTitle()) + "：" + safe(section.getBody()))
                .toList();
    }

    private String toArticleText(ArticleCreationPlan articlePlan) {
        StringBuilder builder = new StringBuilder();
        append(builder, articlePlan.getLead());
        if (articlePlan.getSections() != null) {
            for (ArticleSection section : articlePlan.getSections()) {
                append(builder, "## " + safe(section.getSectionTitle()));
                append(builder, section.getBody());
            }
        }
        append(builder, articlePlan.getConclusion());
        return builder.toString().strip();
    }

    private void append(StringBuilder builder, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        builder.append(text.trim()).append(System.lineSeparator()).append(System.lineSeparator());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未命名段落" : value.trim();
    }
}
