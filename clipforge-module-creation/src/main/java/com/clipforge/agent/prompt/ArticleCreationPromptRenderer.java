package com.clipforge.agent.prompt;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.creation.knowledge.CreativeKnowledge;
import com.clipforge.creation.model.AgentContext;
import com.clipforge.creation.model.AgentPlan;
import com.clipforge.creation.model.AgentStep;
import com.clipforge.creation.prompt.CreationPromptContextFormatter;
import com.clipforge.creation.prompt.RenderedCreationPlanPrompt;
import com.clipforge.creation.prompt.RenderedPrompt;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalResult;
import com.clipforge.material.MaterialSummary;
import com.clipforge.material.video.understanding.VideoUnderstandingCreationContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * ArticleCreationPromptRenderer（图文创作提示词渲染器）。
 */
@Component
public class ArticleCreationPromptRenderer {

    /** resourceLoader（资源加载器）用于读取 Prompt（提示词）文件。 */
    private final ResourceLoader resourceLoader;

    /** memoryPromptRenderer（记忆提示词渲染器）用于渲染记忆上下文。 */
    private final MemoryPromptRenderer memoryPromptRenderer;

    /** formatter（上下文格式化器）用于清洗和格式化动态文本。 */
    private final CreationPromptContextFormatter formatter;

    @Value("${clipforge.prompt.article-creation.system-path:classpath:prompts/article-creation-system.md}")
    private String systemPromptPath = "classpath:prompts/article-creation-system.md";

    @Value("${clipforge.prompt.article-creation.user-path:classpath:prompts/article-creation-user.md}")
    private String userPromptPath = "classpath:prompts/article-creation-user.md";

    @Value("${clipforge.prompt.article-creation.repair-system-path:classpath:prompts/article-creation-repair-system.md}")
    private String repairSystemPromptPath = "classpath:prompts/article-creation-repair-system.md";

    @Value("${clipforge.prompt.article-creation.repair-user-path:classpath:prompts/article-creation-repair-user.md}")
    private String repairUserPromptPath = "classpath:prompts/article-creation-repair-user.md";

    public ArticleCreationPromptRenderer(ResourceLoader resourceLoader,
                                         MemoryPromptRenderer memoryPromptRenderer,
                                         CreationPromptContextFormatter formatter) {
        this.resourceLoader = resourceLoader;
        this.memoryPromptRenderer = memoryPromptRenderer;
        this.formatter = formatter;
    }

    public RenderedCreationPlanPrompt render(AgentContext context, AgentPlan plan, CreativeKnowledge knowledge) {
        String systemTemplate = readTemplate(systemPromptPath);
        String userTemplate = readTemplate(userPromptPath);
        String userPrompt = userTemplate
                .replace("{{goal}}", sanitize(context.getGoal()))
                .replace("{{platform}}", sanitize(context.getPlatform()))
                .replace("{{stylePreference}}", sanitize(formatter.defaultText(context.getStylePreference(), "用户未提供明确风格偏好")))
                .replace("{{memoryContext}}", formatMemoryContext(context))
                .replace("{{platformRules}}", formatter.formatList(knowledge == null ? null : knowledge.getPlatformRules()))
                .replace("{{scriptPatterns}}", formatter.formatList(knowledge == null ? null : knowledge.getScriptPatterns()))
                .replace("{{evaluationHints}}", formatter.formatList(knowledge == null ? null : knowledge.getEvaluationHints()))
                .replace("{{knowledgeRetrievalContext}}", formatKnowledgeRetrievalContext(context, (KnowledgeRetrievalResult) context.getArtifact("KNOWLEDGE_RETRIEVAL_RESULT")))
                .replace("{{materialSummary}}", formatMaterialSummary(context, (MaterialSummary) context.getArtifact("MATERIAL_SUMMARY")))
                .replace("{{videoUnderstandingSummary}}", formatVideoUnderstandingSummary(context, resolveVideoUnderstandingCreationContext(context)))
                .replace("{{planSteps}}", formatPlanSteps(plan == null ? null : plan.getSteps()));
        return RenderedCreationPlanPrompt.builder()
                .systemPrompt(systemTemplate)
                .userPrompt(userPrompt)
                .build();
    }

    public RenderedPrompt renderRepair(String errorMessage, String rawModelOutput) {
        String systemTemplate = readTemplate(repairSystemPromptPath);
        String userTemplate = readTemplate(repairUserPromptPath);
        String userPrompt = userTemplate
                .replace("{{errorMessage}}", sanitize(errorMessage))
                .replace("{{rawModelOutput}}", formatter.sanitizeLongText(rawModelOutput));
        return RenderedPrompt.builder()
                .systemPrompt(systemTemplate)
                .userPrompt(userPrompt)
                .build();
    }

    private String readTemplate(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_ERROR, "图文提示词模板不存在：" + path);
            }
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_ERROR, "图文提示词模板读取失败：" + path, exception);
        }
    }

    private String formatPlanSteps(List<AgentStep> steps) {
        return formatter.formatPlanSteps(steps);
    }

    private String formatMemoryContext(AgentContext context) {
        String compressed = compressedText(context, "COMPRESSED_MEMORY_CONTEXT");
        if (compressed != null) {
            return formatter.sanitizeCompressedText(compressed);
        }
        return memoryPromptRenderer.render(context.getMemoryContext());
    }

    private String formatMaterialSummary(AgentContext context, MaterialSummary materialSummary) {
        String compressed = compressedText(context, "COMPRESSED_IMAGE_SUMMARY");
        if (compressed != null) {
            return formatter.sanitizeCompressedText(compressed);
        }
        if (materialSummary == null || materialSummary.getSummaryText() == null || materialSummary.getSummaryText().isBlank()) {
            return "当前没有可用的图片素材摘要。图文配图建议只能基于已有文本和素材编号保守表达。";
        }
        return formatter.sanitizeLongText(materialSummary.getSummaryText());
    }

    private String formatVideoUnderstandingSummary(AgentContext context, VideoUnderstandingCreationContext videoContext) {
        String compressed = compressedText(context, "COMPRESSED_VIDEO_SUMMARY");
        if (compressed != null) {
            return formatter.sanitizeCompressedText(compressed);
        }
        if (videoContext == null || !videoContext.hasAvailableSummary()) {
            return "当前没有可用的视频素材理解摘要。不要虚构视频画面、字幕或音频内容。";
        }
        return formatter.sanitizeLongText(videoContext.getSummaryText());
    }

    private String formatKnowledgeRetrievalContext(AgentContext context, KnowledgeRetrievalResult result) {
        String compressed = compressedText(context, "COMPRESSED_RAG_CONTEXT");
        if (compressed != null) {
            return formatter.sanitizeCompressedText(compressed);
        }
        if (result == null || result.getPromptContext() == null || result.getPromptContext().isBlank()) {
            return "当前没有可用的知识库检索结果。图文中的具体事实要保持保守。";
        }
        return formatter.sanitizeLongText(result.getPromptContext());
    }

    private VideoUnderstandingCreationContext resolveVideoUnderstandingCreationContext(AgentContext context) {
        Object artifact = context.getArtifact("VIDEO_UNDERSTANDING_CREATION_CONTEXT");
        return artifact instanceof VideoUnderstandingCreationContext videoContext ? videoContext : null;
    }

    private String compressedText(AgentContext context, String key) {
        Object value = context == null ? null : context.getArtifact(key);
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private String sanitize(String text) {
        return formatter.sanitize(text);
    }
}
