package com.clipforge.agent.prompt;

import com.clipforge.creation.model.AgentContext;
import com.clipforge.creation.model.AgentPlan;
import com.clipforge.creation.model.AgentStep;
import com.clipforge.contract.creation.CreationPlanResult;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.creation.evaluation.CreationPlanEvaluationRequest;
import com.clipforge.creation.evaluation.CreationPlanEvaluationResult;
import com.clipforge.material.MaterialSummary;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalResult;
import com.clipforge.creation.knowledge.CreativeKnowledge;
import com.clipforge.creation.prompt.CreationPromptContextFormatter;
import com.clipforge.creation.prompt.RenderedCreationPlanPrompt;
import com.clipforge.creation.prompt.RenderedPrompt;
import com.clipforge.material.video.understanding.VideoUnderstandingCreationContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * CreationPlanPromptRenderer（创作方案提示词渲染器）。
 *
 * <p>该组件负责读取 Prompt Template（提示词模板）文件，并把 AgentContext（智能体上下文）、AgentPlan（智能体计划）和 CreativeKnowledge（创作知识）渲染为模型消息。
 */
@Component
public class CreationPlanPromptRenderer {

    /** resourceLoader（资源加载器）用于读取 classpath（类路径）下的 Prompt（提示词）模板文件。 */
    private final ResourceLoader resourceLoader;

    /** memoryPromptRenderer（记忆提示词渲染器）用于把 MemoryContext（记忆上下文）渲染为可注入提示词片段。 */
    private final MemoryPromptRenderer memoryPromptRenderer;

    /** promptContextFormatter（提示词上下文格式化器）用于处理创作域内对象的文本格式化。 */
    private final CreationPromptContextFormatter promptContextFormatter;

    /** systemPromptPath（系统提示词路径）来自配置文件，默认指向 创作方案生成-系统提示词.md。 */
    @Value("${clipforge.prompt.creation-plan.system-path:classpath:prompts/创作方案生成-系统提示词.md}")
    private String systemPromptPath = "classpath:prompts/创作方案生成-系统提示词.md";

    /** userPromptPath（用户提示词路径）来自配置文件，默认指向 创作方案生成-用户提示词.md。 */
    @Value("${clipforge.prompt.creation-plan.user-path:classpath:prompts/创作方案生成-用户提示词.md}")
    private String userPromptPath = "classpath:prompts/创作方案生成-用户提示词.md";

    /** repairSystemPromptPath（修复系统提示词路径）来自配置文件，默认指向 创作方案修复-系统提示词.md。 */
    @Value("${clipforge.prompt.creation-plan.repair-system-path:classpath:prompts/创作方案修复-系统提示词.md}")
    private String repairSystemPromptPath = "classpath:prompts/创作方案修复-系统提示词.md";

    /** repairUserPromptPath（修复用户提示词路径）来自配置文件，默认指向 创作方案修复-用户提示词.md。 */
    @Value("${clipforge.prompt.creation-plan.repair-user-path:classpath:prompts/创作方案修复-用户提示词.md}")
    private String repairUserPromptPath = "classpath:prompts/创作方案修复-用户提示词.md";

    /** evaluationSystemPromptPath（评估系统提示词路径）来自配置文件，默认指向 创作方案评估-系统提示词.md。 */
    @Value("${clipforge.prompt.creation-plan.evaluation-system-path:classpath:prompts/创作方案评估-系统提示词.md}")
    private String evaluationSystemPromptPath = "classpath:prompts/创作方案评估-系统提示词.md";

    /** evaluationUserPromptPath（评估用户提示词路径）来自配置文件，默认指向 创作方案评估-用户提示词.md。 */
    @Value("${clipforge.prompt.creation-plan.evaluation-user-path:classpath:prompts/创作方案评估-用户提示词.md}")
    private String evaluationUserPromptPath = "classpath:prompts/创作方案评估-用户提示词.md";

    /** revisionSystemPromptPath（修正系统提示词路径）来自配置文件，默认指向 创作方案修正-系统提示词.md。 */
    @Value("${clipforge.prompt.creation-plan.revision-system-path:classpath:prompts/创作方案修正-系统提示词.md}")
    private String revisionSystemPromptPath = "classpath:prompts/创作方案修正-系统提示词.md";

    /** revisionUserPromptPath（修正用户提示词路径）来自配置文件，默认指向 创作方案修正-用户提示词.md。 */
    @Value("${clipforge.prompt.creation-plan.revision-user-path:classpath:prompts/创作方案修正-用户提示词.md}")
    private String revisionUserPromptPath = "classpath:prompts/创作方案修正-用户提示词.md";

    /**
     * 构造函数用于注入 ResourceLoader（资源加载器）。
     *
     * @param resourceLoader 资源加载器
     */
    public CreationPlanPromptRenderer(ResourceLoader resourceLoader) {
        // 调用完整构造函数，并创建默认记忆提示词渲染器以兼容早期单元测试。
        this(resourceLoader, new MemoryPromptRenderer(), new CreationPromptContextFormatter());
    }

    /**
     * 构造函数用于注入 ResourceLoader（资源加载器）和 MemoryPromptRenderer（记忆提示词渲染器）。
     *
     * @param resourceLoader 资源加载器
     * @param memoryPromptRenderer 记忆提示词渲染器
     */
    public CreationPlanPromptRenderer(ResourceLoader resourceLoader, MemoryPromptRenderer memoryPromptRenderer) {
        this(resourceLoader, memoryPromptRenderer, new CreationPromptContextFormatter());
    }

    /**
     * 构造函数用于注入完整 Prompt（提示词）渲染依赖。
     *
     * @param resourceLoader 资源加载器
     * @param memoryPromptRenderer 记忆提示词渲染器
     * @param promptContextFormatter 创作提示词上下文格式化器
     */
    @Autowired
    public CreationPlanPromptRenderer(ResourceLoader resourceLoader,
                                      MemoryPromptRenderer memoryPromptRenderer,
                                      CreationPromptContextFormatter promptContextFormatter) {
        // 保存资源加载器，后续按配置路径读取模板文件。
        this.resourceLoader = resourceLoader;
        // 保存记忆提示词渲染器，后续渲染动态记忆上下文。
        this.memoryPromptRenderer = memoryPromptRenderer;
        // 保存创作提示词上下文格式化器，后续委托它处理纯格式化逻辑。
        this.promptContextFormatter = promptContextFormatter;
    }

    /**
     * render（渲染）用于把当前创作任务上下文渲染为 system（系统角色）和 user（用户角色）两段提示词。
     *
     * @param context AgentContext（智能体上下文）
     * @param plan AgentPlan（智能体计划）
     * @param knowledge CreativeKnowledge（创作知识）
     * @return 已渲染创作方案提示词
     */
    public RenderedCreationPlanPrompt render(AgentContext context, AgentPlan plan, CreativeKnowledge knowledge) {
        // 读取 system（系统角色）提示词模板。
        String systemTemplate = readTemplate(systemPromptPath);
        // 读取 user（用户角色）提示词模板。
        String userTemplate = readTemplate(userPromptPath);
        // 渲染 user（用户角色）提示词中的动态变量。
        String userPrompt = userTemplate
                // 替换创作目标。
                .replace("{{goal}}", sanitize(context.getGoal()))
                // 替换目标平台。
                .replace("{{platform}}", sanitize(context.getPlatform()))
                // 替换风格偏好，空值时使用明确说明。
                .replace("{{stylePreference}}", sanitize(defaultText(context.getStylePreference(), "用户未提供明确风格偏好")))
                // 替换记忆上下文。
                .replace("{{memoryContext}}", formatMemoryContext(context))
                // 替换平台规则列表。
                .replace("{{platformRules}}", formatList(knowledge.getPlatformRules()))
                // 替换脚本结构列表。
                .replace("{{scriptPatterns}}", formatList(knowledge.getScriptPatterns()))
                // 替换评估提示列表。
                .replace("{{evaluationHints}}", formatList(knowledge.getEvaluationHints()))
                // 替换检索增强上下文。
                .replace("{{knowledgeRetrievalContext}}", formatKnowledgeRetrievalContext(context, (KnowledgeRetrievalResult) context.getArtifact("KNOWLEDGE_RETRIEVAL_RESULT")))
                // 替换素材摘要。
                .replace("{{materialSummary}}", formatMaterialSummary(context, (MaterialSummary) context.getArtifact("MATERIAL_SUMMARY")))
                // 替换视频素材摘要。
                .replace("{{videoUnderstandingSummary}}", formatVideoUnderstandingSummary(context, resolveVideoUnderstandingCreationContext(context)))
                // 替换智能体计划步骤列表。
                .replace("{{planSteps}}", formatPlanSteps(plan.getSteps()));
        // 使用 Builder（建造者模式）组装已渲染提示词。
        return RenderedCreationPlanPrompt.builder()
                // 设置系统提示词。
                .systemPrompt(systemTemplate)
                // 设置用户提示词。
                .userPrompt(userPrompt)
                // 构建已渲染提示词对象。
                .build();
    }

    /**
     * renderRepair（渲染修复提示词）用于把模型输出错误原因和原始输出渲染为输出修复 Prompt（提示词）。
     *
     * @param errorMessage 错误消息
     * @param rawModelOutput 模型原始输出
     * @return 已渲染提示词
     */
    public RenderedPrompt renderRepair(String errorMessage, String rawModelOutput) {
        // 读取修复 system（系统角色）提示词模板。
        String systemTemplate = readTemplate(repairSystemPromptPath);
        // 读取修复 user（用户角色）提示词模板。
        String userTemplate = readTemplate(repairUserPromptPath);
        // 渲染修复 user（用户角色）提示词中的动态变量。
        String userPrompt = userTemplate
                // 替换错误原因。
                .replace("{{errorMessage}}", sanitize(errorMessage))
                // 替换原始模型输出。
                .replace("{{rawModelOutput}}", sanitizeLongText(rawModelOutput));
        // 使用 Builder（建造者模式）组装修复提示词。
        return RenderedPrompt.builder()
                // 设置系统提示词。
                .systemPrompt(systemTemplate)
                // 设置用户提示词。
                .userPrompt(userPrompt)
                // 构建已渲染提示词对象。
                .build();
    }

    /**
     * renderEvaluation（渲染评估提示词）用于把创作方案评估请求渲染为 LLM-as-Judge（大模型裁判）提示词。
     *
     * @param request 创作方案评估请求
     * @return 已渲染评估提示词
     */
    public RenderedPrompt renderEvaluation(CreationPlanEvaluationRequest request) {
        // 读取评估 system（系统角色）提示词模板。
        String systemTemplate = readTemplate(evaluationSystemPromptPath);
        // 读取评估 user（用户角色）提示词模板。
        String userTemplate = readTemplate(evaluationUserPromptPath);
        // 读取智能体上下文。
        AgentContext context = request.getContext();
        // 读取创作知识。
        CreativeKnowledge knowledge = request.getCreativeKnowledge();
        // 渲染评估 user（用户角色）提示词中的动态变量。
        String userPrompt = userTemplate
                // 替换创作目标。
                .replace("{{goal}}", sanitize(context.getGoal()))
                // 替换目标平台。
                .replace("{{platform}}", sanitize(context.getPlatform()))
                // 替换风格偏好。
                .replace("{{stylePreference}}", sanitize(defaultText(context.getStylePreference(), "用户未提供明确风格偏好")))
                // 替换记忆上下文。
                .replace("{{memoryContext}}", formatMemoryContext(context))
                // 替换平台规则。
                .replace("{{platformRules}}", formatList(knowledge == null ? null : knowledge.getPlatformRules()))
                // 替换脚本结构建议。
                .replace("{{scriptPatterns}}", formatList(knowledge == null ? null : knowledge.getScriptPatterns()))
                // 替换评估提示。
                .replace("{{evaluationHints}}", formatList(knowledge == null ? null : knowledge.getEvaluationHints()))
                // 替换检索增强上下文。
                .replace("{{knowledgeRetrievalContext}}", formatKnowledgeRetrievalContext(context, (KnowledgeRetrievalResult) context.getArtifact("KNOWLEDGE_RETRIEVAL_RESULT")))
                // 替换素材摘要。
                .replace("{{materialSummary}}", formatMaterialSummary(context, (MaterialSummary) context.getArtifact("MATERIAL_SUMMARY")))
                // 替换视频素材摘要。
                .replace("{{videoUnderstandingSummary}}", formatVideoUnderstandingSummary(context, resolveVideoUnderstandingCreationContext(context)))
                // 替换待评估创作方案。
                .replace("{{creationPlanResult}}", formatCreationPlanResult(request.getPlanResult()));
        // 使用 Builder（建造者模式）组装评估提示词。
        return RenderedPrompt.builder()
                // 设置系统提示词。
                .systemPrompt(systemTemplate)
                // 设置用户提示词。
                .userPrompt(userPrompt)
                // 构建已渲染提示词对象。
                .build();
    }

    /**
     * renderRevision（渲染修正提示词）用于把原始方案和评估结果渲染为有限 Reflection（反思修正）提示词。
     *
     * @param context 智能体上下文
     * @param originalResult 原始创作方案结果
     * @param evaluationResult 创作方案评估结果
     * @param knowledge 创作知识
     * @return 已渲染修正提示词
     */
    public RenderedPrompt renderRevision(AgentContext context, CreationPlanResult originalResult, CreationPlanEvaluationResult evaluationResult, CreativeKnowledge knowledge) {
        // 读取修正 system（系统角色）提示词模板。
        String systemTemplate = readTemplate(revisionSystemPromptPath);
        // 读取修正 user（用户角色）提示词模板。
        String userTemplate = readTemplate(revisionUserPromptPath);
        // 渲染修正 user（用户角色）提示词中的动态变量。
        String userPrompt = userTemplate
                // 替换创作目标。
                .replace("{{goal}}", sanitize(context.getGoal()))
                // 替换目标平台。
                .replace("{{platform}}", sanitize(context.getPlatform()))
                // 替换风格偏好。
                .replace("{{stylePreference}}", sanitize(defaultText(context.getStylePreference(), "用户未提供明确风格偏好")))
                // 替换记忆上下文。
                .replace("{{memoryContext}}", formatMemoryContext(context))
                // 替换平台规则。
                .replace("{{platformRules}}", formatList(knowledge == null ? null : knowledge.getPlatformRules()))
                // 替换脚本结构建议。
                .replace("{{scriptPatterns}}", formatList(knowledge == null ? null : knowledge.getScriptPatterns()))
                // 替换检索增强上下文。
                .replace("{{knowledgeRetrievalContext}}", formatKnowledgeRetrievalContext(context, (KnowledgeRetrievalResult) context.getArtifact("KNOWLEDGE_RETRIEVAL_RESULT")))
                // 替换素材摘要。
                .replace("{{materialSummary}}", formatMaterialSummary(context, (MaterialSummary) context.getArtifact("MATERIAL_SUMMARY")))
                // 替换视频素材摘要。
                .replace("{{videoUnderstandingSummary}}", formatVideoUnderstandingSummary(context, resolveVideoUnderstandingCreationContext(context)))
                // 替换原始创作方案。
                .replace("{{originalCreationPlanResult}}", formatCreationPlanResult(originalResult))
                // 替换评估结果。
                .replace("{{evaluationResult}}", formatEvaluationResult(evaluationResult));
        // 使用 Builder（建造者模式）组装修正提示词。
        return RenderedPrompt.builder()
                // 设置系统提示词。
                .systemPrompt(systemTemplate)
                // 设置用户提示词。
                .userPrompt(userPrompt)
                // 构建已渲染提示词。
                .build();
    }

    /**
     * readTemplate（读取模板）用于从配置路径读取 Prompt（提示词）模板。
     *
     * @param path 模板路径
     * @return 模板文本
     */
    private String readTemplate(String path) {
        try {
            // 根据路径获取 Spring Resource（资源）对象。
            Resource resource = resourceLoader.getResource(path);
            // 如果资源不存在，抛出业务异常，避免后续模型调用使用空提示词。
            if (!resource.exists()) {
                // 抛出提示词模板错误。
                throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_ERROR, "提示词模板不存在：" + path);
            }
            // 使用 UTF-8（统一字符编码）读取模板内容。
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            // 读取失败时包装为业务异常，交给统一异常处理器。
            throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_ERROR, "提示词模板读取失败：" + path, exception);
        }
    }

    /**
     * formatList（格式化列表）用于把创作知识列表转换为带稳定编号的 Prompt（提示词）文本。
     *
     * @param items 待格式化列表
     * @return 格式化后的文本
     */
    private String formatList(List<String> items) {
        return promptContextFormatter.formatList(items);
    }

    /**
     * formatPlanSteps（格式化计划步骤）用于把 AgentPlan（智能体计划）步骤转换为 Prompt（提示词）文本。
     *
     * @param steps 智能体计划步骤列表
     * @return 格式化后的计划步骤文本
     */
    private String formatPlanSteps(List<AgentStep> steps) {
        return promptContextFormatter.formatPlanSteps(steps);
    }

    /**
     * formatCreationPlanResult（格式化创作方案结果）用于把待评估方案转换为稳定文本。
     *
     * @param result 创作方案结果
     * @return 格式化后的创作方案文本
     */
    private String formatCreationPlanResult(CreationPlanResult result) {
        return promptContextFormatter.formatCreationPlanResult(result);
    }

    /**
     * formatMaterialSummary（格式化素材摘要）用于把图片理解摘要注入 Prompt（提示词）。
     *
     * @param materialSummary 素材摘要
     * @return 格式化后的素材摘要文本
     */
    private String formatMaterialSummary(AgentContext context, MaterialSummary materialSummary) {
        String compressedText = compressedText(context, "COMPRESSED_IMAGE_SUMMARY");
        if (compressedText != null) {
            return sanitizeCompressedText(compressedText);
        }
        // 如果当前没有素材摘要，则返回明确说明，让模型不要虚构图片内容。
        if (materialSummary == null || materialSummary.getSummaryText() == null || materialSummary.getSummaryText().isBlank()) {
            // 返回无素材摘要说明。
            return "当前没有可用的图片素材摘要。生成、评估或修正方案时，不要虚构图片中不存在的画面信息。";
        }
        // 返回经过长文本清洗的素材摘要。
        return sanitizeLongText(materialSummary.getSummaryText());
    }

    /**
     * formatVideoUnderstandingSummary（格式化视频理解摘要）用于把已保存的视频理解结果注入 Prompt（提示词）。
     *
     * @param videoContext 视频理解创作上下文
     * @return 格式化后的视频摘要文本
     */
    private String formatVideoUnderstandingSummary(AgentContext context, VideoUnderstandingCreationContext videoContext) {
        String compressedText = compressedText(context, "COMPRESSED_VIDEO_SUMMARY");
        if (compressedText != null) {
            return sanitizeCompressedText(compressedText);
        }
        // 如果当前没有视频理解上下文，则返回明确说明，让模型不要虚构视频内容。
        if (videoContext == null) {
            // 返回无视频摘要说明。
            return "当前没有可用的视频素材理解摘要。生成、评估或修正方案时，不要虚构视频片段、关键画面或音频内容。";
        }
        // 创建字符串构建器。
        StringBuilder builder = new StringBuilder();
        // 如果存在可用摘要，则先写入摘要文本。
        if (videoContext.hasAvailableSummary()) {
            // 写入经过长文本清洗的视频摘要。
            builder.append(sanitizeLongText(videoContext.getSummaryText()));
        } else {
            // 写入无摘要说明。
            builder.append("当前没有可用的视频素材理解摘要。生成、评估或修正方案时，不要虚构视频片段、关键画面或音频内容。");
        }
        // 如果存在警告，则追加警告，帮助模型保持保守。
        if (videoContext.getWarnings() != null && !videoContext.getWarnings().isEmpty()) {
            // 追加警告标题。
            builder.append(System.lineSeparator()).append("视频摘要警告：").append(System.lineSeparator());
            // 遍历警告。
            for (String warning : videoContext.getWarnings()) {
                // 追加单条警告。
                builder.append("- ").append(sanitize(warning)).append(System.lineSeparator());
            }
        }
        // 返回格式化文本。
        return builder.toString().stripTrailing();
    }

    /**
     * resolveVideoUnderstandingCreationContext（解析视频理解创作上下文）用于安全读取上下文中的视频摘要对象。
     *
     * @param context 智能体上下文
     * @return 视频理解创作上下文
     */
    private VideoUnderstandingCreationContext resolveVideoUnderstandingCreationContext(AgentContext context) {
        // 读取中间产物。
        Object artifact = context.getArtifact("VIDEO_UNDERSTANDING_CREATION_CONTEXT");
        // 如果类型正确，则返回对象。
        if (artifact instanceof VideoUnderstandingCreationContext videoContext) {
            // 返回视频上下文。
            return videoContext;
        }
        // 其他情况返回空引用。
        return null;
    }

    /**
     * formatKnowledgeRetrievalContext（格式化知识检索上下文）用于把 RAG（检索增强生成）命中内容注入 Prompt（提示词）。
     *
     * @param result 知识检索结果
     * @return 格式化后的知识检索上下文
     */
    private String formatKnowledgeRetrievalContext(AgentContext context, KnowledgeRetrievalResult result) {
        String compressedText = compressedText(context, "COMPRESSED_RAG_CONTEXT");
        if (compressedText != null) {
            return sanitizeCompressedText(compressedText);
        }
        // 如果没有检索结果，则明确告知模型不要编造知识库事实。
        if (result == null) {
            return "当前没有可用的知识库检索结果。若知识库未命中，请不要编造具体事实，只能基于已知目标和素材给出保守方案。";
        }
        // 创建字符串构建器。
        StringBuilder builder = new StringBuilder();
        // 追加检索来源。
        builder.append("检索来源：").append(defaultText(result.getSource() == null ? null : result.getSource().value(), "未提供")).append(System.lineSeparator());
        // 追加是否降级。
        builder.append("是否降级：").append(result.isFallbackUsed()).append(System.lineSeparator());
        // 追加降级原因。
        if (result.getFallbackReason() != null && !result.getFallbackReason().isBlank()) {
            builder.append("降级原因：").append(sanitize(result.getFallbackReason())).append(System.lineSeparator());
        }
        // 追加适合最终模型阅读的知识标题，避免把内部检索输入和调试信息塞进 Prompt（提示词）。
        builder.append(result.isFallbackUsed() ? "静态兜底知识：" : "知识库命中内容：").append(System.lineSeparator());
        if (result.getPromptContext() == null || result.getPromptContext().isBlank()) {
            // 如果没有命中内容，则直接提示。
            builder.append("- 暂无命中内容，请保持保守表达。");
        } else {
            // 直接注入命中上下文，但先做长文本清洗。
            builder.append(sanitizeLongText(result.getPromptContext()));
        }
        // 返回格式化文本，并去掉末尾多余空白。
        return builder.toString().stripTrailing();
    }

    /**
     * formatEvaluationResult（格式化评估结果）用于把结构化评估结果转换为修正 Prompt（提示词）文本。
     *
     * @param result 创作方案评估结果
     * @return 格式化后的评估结果文本
     */
    private String formatEvaluationResult(CreationPlanEvaluationResult result) {
        return promptContextFormatter.formatEvaluationResult(result);
    }

    /**
     * sanitize（清洗文本）用于降低 Prompt Injection（提示词注入）和异常长输入风险。
     *
     * @param text 原始文本
     * @return 清洗后的文本
     */
    private String sanitize(String text) {
        return promptContextFormatter.sanitize(text);
    }

    /**
     * sanitizeLongText（清洗长文本）用于处理模型原始输出，保留更多内容给修复模型参考。
     *
     * @param text 原始文本
     * @return 清洗后的长文本
     */
    private String sanitizeLongText(String text) {
        return promptContextFormatter.sanitizeLongText(text);
    }

    /**
     * defaultText（默认文本）用于处理空字符串和空引用。
     *
     * @param text 原始文本
     * @param defaultValue 默认值
     * @return 非空文本
     */
    /**
     * formatMemoryContext（格式化记忆上下文）优先使用压缩后的 Memory（记忆）覆盖文本。
     *
     * @param context AgentContext（智能体上下文）
     * @return 可注入 Prompt（提示词）的记忆文本
     */
    /**
     * sanitizeCompressedText（清洗压缩文本）用于处理已经被 Prompt Compression（提示词压缩）裁剪过的文本。
     *
     * @param text 压缩后的文本
     * @return 清洗后的压缩文本
     */
    private String sanitizeCompressedText(String text) {
        return promptContextFormatter.sanitizeCompressedText(text);
    }

    /**
     * formatMemoryContext（格式化记忆上下文）优先使用压缩后的 Memory（记忆）覆盖文本。
     *
     * @param context AgentContext（智能体上下文）
     * @return 可注入 Prompt（提示词）的记忆文本
     */
    private String formatMemoryContext(AgentContext context) {
        String compressedText = compressedText(context, "COMPRESSED_MEMORY_CONTEXT");
        if (compressedText != null) {
            return sanitizeCompressedText(compressedText);
        }
        return memoryPromptRenderer.render(context.getMemoryContext());
    }

    /**
     * compressedText（压缩覆盖文本）用于读取本次模型调用前的裁剪结果。
     *
     * @param context AgentContext（智能体上下文）
     * @param key 覆盖文本键
     * @return 非空覆盖文本；不存在时返回 null
     */
    private String compressedText(AgentContext context, String key) {
        if (context == null) {
            return null;
        }
        Object value = context.getArtifact(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }

    private String defaultText(String text, String defaultValue) {
        return promptContextFormatter.defaultText(text, defaultValue);
    }
}
