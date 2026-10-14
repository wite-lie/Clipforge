package com.clipforge.creation.export;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.contract.creation.ArticleCreationPlan;
import com.clipforge.contract.creation.ArticleImageSlot;
import com.clipforge.contract.creation.ArticleSection;
import com.clipforge.contract.creation.ArticleSourceEvidence;
import com.clipforge.contract.creation.CreationOutputType;
import com.clipforge.contract.creation.CreationPlanResult;
import com.clipforge.memory.infrastructure.persistence.entity.CreationOutputEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * CreationMarkdownRenderer（创作 Markdown 渲染器）。
 *
 * <p>该组件只做确定性文本渲染，不调用大模型。
 */
@Component
public class CreationMarkdownRenderer {

    /** TIMESTAMP_FORMATTER（时间戳格式化器）用于生成稳定文件名。 */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** objectMapper（对象映射器）用于把 payloadJson（产物内容 JSON）还原为当前结构。 */
    private final ObjectMapper objectMapper;

    public CreationMarkdownRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * render（渲染）用于把通用创作产物渲染为 Markdown 文档。
     *
     * @param outputEntity 创作产物实体
     * @return Markdown 文档
     */
    public CreationMarkdownDocument render(CreationOutputEntity outputEntity) {
        if (outputEntity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "创作产物不存在，无法导出 Markdown");
        }
        CreationOutputType outputType = CreationOutputType.resolveOrDefault(outputEntity.getOutputType());
        Object payload = parsePayload(outputType, outputEntity.getPayloadJson());
        CreationPlanResult result = payload instanceof CreationPlanResult planResult ? planResult : null;
        ArticleCreationPlan articlePlan = payload instanceof ArticleCreationPlan plan ? plan : null;
        String title = outputType == CreationOutputType.ARTICLE_PLAN
                ? firstNonBlank(first(articlePlan.getTitleCandidates()), articlePlan.getTheme(), "图文创作方案")
                : firstNonBlank(first(result.getTitleCandidates()), result.getTheme(), "创作方案");
        String fileName = buildFileName(outputEntity.getRunId(), outputType);
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(escapeLine(title)).append("\n\n");
        markdown.append("> 产物类型：").append(outputType.name())
                .append("  \n> 运行编号：").append(safe(outputEntity.getRunId()))
                .append("  \n> 产物编号：").append(safe(outputEntity.getOutputId()))
                .append("  \n> 结构版本：").append(safe(outputEntity.getSchemaVersion()))
                .append("\n\n");
        if (outputType == CreationOutputType.ARTICLE_PLAN) {
            renderArticlePlan(markdown, articlePlan);
        } else {
            renderSummary(markdown, result);
            renderVideoPlan(markdown, result);
            renderPublish(markdown, result);
            renderQuality(markdown, result);
        }
        return new CreationMarkdownDocument(fileName, markdown.toString());
    }

    private void renderSummary(StringBuilder markdown, CreationPlanResult result) {
        markdown.append("## 核心信息\n\n");
        appendBullet(markdown, "创作主题", result.getTheme());
        appendBullet(markdown, "目标受众", result.getTargetAudience());
        appendBullet(markdown, "开头钩子", result.getHook());
        markdown.append("\n");
    }

    private void renderVideoPlan(StringBuilder markdown, CreationPlanResult result) {
        markdown.append("## 分镜脚本\n\n");
        appendNumberedList(markdown, result.getStoryboard(), "暂无分镜脚本。");
        markdown.append("\n## 旁白脚本\n\n");
        markdown.append(blankAsDefault(result.getVoiceoverScript(), "暂无旁白脚本。")).append("\n\n");
        markdown.append("## 字幕建议\n\n");
        appendBulletList(markdown, result.getSubtitleSuggestions(), "暂无字幕建议。");
        markdown.append("\n");
    }

    private void renderArticlePlan(StringBuilder markdown, ArticleCreationPlan articlePlan) {
        markdown.append("## 核心信息\n\n");
        appendBullet(markdown, "创作主题", articlePlan.getTheme());
        appendBullet(markdown, "目标受众", articlePlan.getTargetAudience());
        appendBullet(markdown, "摘要", articlePlan.getSummary());
        markdown.append("\n## 标题候选\n\n");
        appendBulletList(markdown, articlePlan.getTitleCandidates(), "暂无标题候选。");
        markdown.append("\n## 导语\n\n");
        markdown.append(blankAsDefault(articlePlan.getLead(), "暂无导语。")).append("\n\n");
        markdown.append("## 正文\n\n");
        if (articlePlan.getSections() == null || articlePlan.getSections().isEmpty()) {
            markdown.append("暂无正文段落。\n\n");
        } else {
            for (ArticleSection section : articlePlan.getSections()) {
                markdown.append("### ").append(escapeLine(section.getSectionTitle())).append("\n\n");
                markdown.append(blankAsDefault(section.getBody(), "暂无正文。")).append("\n\n");
                markdown.append("要点：\n");
                appendBulletList(markdown, section.getKeyPoints(), "暂无要点。");
                if (section.getSuggestedMaterialIds() != null && !section.getSuggestedMaterialIds().isEmpty()) {
                    markdown.append("\n建议素材：\n");
                    appendBulletList(markdown, section.getSuggestedMaterialIds(), "暂无建议素材。");
                }
                appendImageSlots(markdown, section.getImageSlots(), "段落配图建议");
                appendEvidence(markdown, section.getSourceEvidence());
                markdown.append("\n");
            }
        }
        appendImageSlots(markdown, articlePlan.getImageSlots(), "全文配图规划");
        markdown.append("## 结尾\n\n");
        markdown.append(blankAsDefault(articlePlan.getConclusion(), "暂无结尾。")).append("\n\n");
        markdown.append("## 互动引导\n\n");
        markdown.append(blankAsDefault(articlePlan.getInteractionPrompt(), "暂无互动引导。")).append("\n\n");
        markdown.append("## 发布信息\n\n");
        appendBullet(markdown, "发布文案", articlePlan.getPublishCopy());
        markdown.append("\n### 话题标签\n\n");
        appendBulletList(markdown, articlePlan.getHashtags(), "暂无话题标签。");
        appendEvidence(markdown, articlePlan.getSourceEvidence());
        markdown.append("\n## 风险提示\n\n");
        appendBulletList(markdown, articlePlan.getRiskWarnings(), "暂无风险提示。");
        markdown.append("\n## 质量复盘\n\n");
        markdown.append("- 综合评分：").append(articlePlan.getScore()).append("\n");
        markdown.append("- 改进建议：\n");
        appendIndentedBulletList(markdown, articlePlan.getImprovementSuggestions(), "暂无改进建议。");
    }

    private void renderPublish(StringBuilder markdown, CreationPlanResult result) {
        markdown.append("## 发布信息\n\n");
        markdown.append("### 标题候选\n\n");
        appendBulletList(markdown, result.getTitleCandidates(), "暂无标题候选。");
        markdown.append("\n### 发布文案\n\n");
        markdown.append(blankAsDefault(result.getPublishCopy(), "暂无发布文案。")).append("\n\n");
        markdown.append("### 话题标签\n\n");
        appendBulletList(markdown, result.getHashtags(), "暂无话题标签。");
        markdown.append("\n");
    }

    private void renderQuality(StringBuilder markdown, CreationPlanResult result) {
        markdown.append("## 质量复盘\n\n");
        markdown.append("- 综合评分：").append(result.getScore()).append("\n");
        markdown.append("- 改进建议：\n");
        appendIndentedBulletList(markdown, result.getImprovementSuggestions(), "暂无改进建议。");
    }

    private void appendImageSlots(StringBuilder markdown, List<ArticleImageSlot> imageSlots, String title) {
        if (imageSlots == null || imageSlots.isEmpty()) {
            return;
        }
        markdown.append("\n").append(title).append("：\n");
        for (ArticleImageSlot slot : imageSlots) {
            if (slot == null) {
                continue;
            }
            markdown.append("- ")
                    .append(blankAsDefault(slot.getPlacement(), "未指定位置"))
                    .append(" / ")
                    .append(blankAsDefault(slot.getSourceType(), "NONE"))
                    .append("：")
                    .append(blankAsDefault(slot.getReason(), slot.getImagePrompt() == null ? "暂无配图说明" : slot.getImagePrompt()))
                    .append("\n");
            if (slot.getRecommendedMaterialIds() != null && !slot.getRecommendedMaterialIds().isEmpty()) {
                markdown.append("  - 推荐素材：").append(String.join("、", slot.getRecommendedMaterialIds())).append("\n");
            }
            if (slot.getBoundMaterialId() != null && !slot.getBoundMaterialId().isBlank()) {
                markdown.append("  - 已绑定素材：").append(slot.getBoundMaterialId().trim()).append("\n");
            }
            if (slot.getImagePrompt() != null && !slot.getImagePrompt().isBlank()) {
                markdown.append("  - 配图提示：").append(slot.getImagePrompt().trim()).append("\n");
            }
            if (slot.getStatus() != null && !slot.getStatus().isBlank()) {
                markdown.append("  - 状态：").append(slot.getStatus().trim()).append("\n");
            }
        }
    }

    private CreationPlanResult parseCreationPlanResult(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "创作产物内容为空，无法导出 Markdown");
        }
        try {
            return objectMapper.readValue(payloadJson, CreationPlanResult.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创作产物 JSON 解析失败，无法导出 Markdown", exception);
        }
    }

    private Object parsePayload(CreationOutputType outputType, String payloadJson) {
        if (outputType == CreationOutputType.ARTICLE_PLAN) {
            if (payloadJson == null || payloadJson.isBlank()) {
                throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "图文创作产物内容为空，无法导出 Markdown");
            }
            try {
                return objectMapper.readValue(payloadJson, ArticleCreationPlan.class);
            } catch (JsonProcessingException exception) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图文创作产物 JSON 解析失败，无法导出 Markdown", exception);
            }
        }
        return parseCreationPlanResult(payloadJson);
    }

    private void appendEvidence(StringBuilder markdown, List<ArticleSourceEvidence> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return;
        }
        markdown.append("\n证据依据：\n");
        for (ArticleSourceEvidence evidence : evidences) {
            markdown.append("- ")
                    .append(blankAsDefault(evidence.getSourceType(), "UNKNOWN"))
                    .append(evidence.getSourceId() == null || evidence.getSourceId().isBlank() ? "" : " / " + evidence.getSourceId().trim())
                    .append("：")
                    .append(blankAsDefault(evidence.getQuote(), "暂无引用"))
                    .append("\n");
        }
    }

    private void appendBullet(StringBuilder markdown, String label, String value) {
        markdown.append("- ").append(label).append("：").append(blankAsDefault(value, "暂无")).append("\n");
    }

    private void appendNumberedList(StringBuilder markdown, List<String> values, String emptyText) {
        if (values == null || values.isEmpty()) {
            markdown.append(emptyText).append("\n");
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            markdown.append(i + 1).append(". ").append(blankAsDefault(values.get(i), "暂无")).append("\n");
        }
    }

    private void appendBulletList(StringBuilder markdown, List<String> values, String emptyText) {
        if (values == null || values.isEmpty()) {
            markdown.append(emptyText).append("\n");
            return;
        }
        for (String value : values) {
            markdown.append("- ").append(blankAsDefault(value, "暂无")).append("\n");
        }
    }

    private void appendIndentedBulletList(StringBuilder markdown, List<String> values, String emptyText) {
        if (values == null || values.isEmpty()) {
            markdown.append("  - ").append(emptyText).append("\n");
            return;
        }
        for (String value : values) {
            markdown.append("  - ").append(blankAsDefault(value, "暂无")).append("\n");
        }
    }

    private String buildFileName(String runId, CreationOutputType outputType) {
        String raw = safe(runId) + "-" + outputType.name() + "-" + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + ".md";
        return raw.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String blankAsDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private String escapeLine(String value) {
        return blankAsDefault(value, "创作方案").replace("\r", " ").replace("\n", " ");
    }
}
