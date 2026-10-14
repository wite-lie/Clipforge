package com.clipforge.prompt.token;

import com.clipforge.creation.model.AgentContext;
import com.clipforge.creation.prompt.RenderedCreationPlanPrompt;
import com.clipforge.agent.prompt.MemoryPromptRenderer;
import com.clipforge.material.MaterialSummary;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalResult;
import com.clipforge.material.video.understanding.VideoUnderstandingCreationContext;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * CreationPlanPromptTokenEstimateService（创作方案提示词词元估算服务）。
 *
 * <p>该服务负责把创作方案生成前的上下文拆成 PromptSection（提示词片段），再调用 PromptTokenEstimator（提示词词元估算器）生成估算结果。
 */
@Component
@RequiredArgsConstructor
public class CreationPlanPromptTokenEstimateService {

    /** promptTokenEstimator（提示词词元估算器）用于执行本地启发式 Token（词元）估算。 */
    private final PromptTokenEstimator promptTokenEstimator;

    /** memoryPromptRenderer（记忆提示词渲染器）用于把 MemoryContext（记忆上下文）转换为和真实 Prompt（提示词）一致的文本片段。 */
    private final MemoryPromptRenderer memoryPromptRenderer;

    /**
     * estimateCreationPlanPrompt（估算创作方案提示词）用于在 LLM（大语言模型）调用前估算 Prompt（提示词）长度。
     *
     * @param context 智能体上下文
     * @param renderedPrompt 已渲染创作方案提示词
     * @return 估算结果
     */
    public PromptTokenEstimateResult estimateCreationPlanPrompt(AgentContext context, RenderedCreationPlanPrompt renderedPrompt) {
        // 构建创作方案提示词片段。
        List<PromptSection> sections = buildSections(context, renderedPrompt);
        // 调用估算器生成结果。
        return promptTokenEstimator.estimate(sections);
    }

    /**
     * buildSections（构建片段）用于生成真实总数片段和诊断片段。
     *
     * @param context 智能体上下文
     * @param renderedPrompt 已渲染创作方案提示词
     * @return 提示词片段列表
     */
    private List<PromptSection> buildSections(AgentContext context, RenderedCreationPlanPrompt renderedPrompt) {
        // 创建片段列表。
        List<PromptSection> sections = new ArrayList<>();
        // 添加真实发送给模型的 system（系统角色）提示词。
        sections.add(section(PromptSectionType.SYSTEM_PROMPT, "systemPrompt（系统提示词）", renderedPrompt.getSystemPrompt(), true));
        // 添加真实发送给模型的 user（用户角色）提示词。
        sections.add(section(PromptSectionType.USER_PROMPT, "userPrompt（完整用户提示词）", renderedPrompt.getUserPrompt(), true));
        // 添加用户输入诊断片段。
        sections.add(section(PromptSectionType.USER_REQUEST, "用户请求", buildUserRequestText(context), false));
        // 添加 RAG（检索增强生成）诊断片段。
        sections.add(section(PromptSectionType.RAG_CONTEXT, "RAG（检索增强生成）上下文", buildRagContextText(context), false));
        // 添加 Memory（记忆）诊断片段。
        sections.add(section(PromptSectionType.MEMORY_CONTEXT, "Memory（记忆）上下文", buildMemoryContextText(context), false));
        // 添加图片摘要诊断片段。
        sections.add(section(PromptSectionType.IMAGE_SUMMARY, "图片理解摘要", buildImageSummaryText(context), false));
        // 添加视频摘要诊断片段。
        sections.add(section(PromptSectionType.VIDEO_SUMMARY, "视频理解摘要", buildVideoSummaryText(context), false));
        // 当前第一版没有单独直接注入会话历史，保留空诊断片段给后续多轮上下文治理使用。
        sections.add(section(PromptSectionType.CONVERSATION_HISTORY, "会话历史", "", false));
        // 输出结构约束主要位于 systemPrompt（系统提示词）中，第一版先通过诊断片段估算相关关键字附近文本。
        sections.add(section(PromptSectionType.OUTPUT_SCHEMA, "输出结构约束", buildOutputSchemaText(renderedPrompt), false));
        // 返回片段列表。
        return sections;
    }

    /**
     * section（片段）用于快速构造 PromptSection（提示词片段）。
     *
     * @param type 片段类型
     * @param name 片段名称
     * @param content 片段内容
     * @param includedInTotal 是否计入真实总数
     * @return 提示词片段
     */
    private PromptSection section(PromptSectionType type, String name, String content, boolean includedInTotal) {
        // 使用 Builder（建造者模式）构造片段对象。
        return PromptSection.builder()
                // 设置类型。
                .type(type)
                // 设置名称。
                .name(name)
                // 设置内容。
                .content(content == null ? "" : content)
                // 设置是否计入总数。
                .includedInTotal(includedInTotal)
                // 构建片段。
                .build();
    }

    /**
     * buildUserRequestText（构建用户请求文本）用于诊断用户原始输入占用。
     *
     * @param context 智能体上下文
     * @return 用户请求文本
     */
    private String buildUserRequestText(AgentContext context) {
        // 组装用户请求核心字段。
        return "创作目标：" + safe(context.getGoal()) + "\n"
                // 追加目标平台。
                + "目标平台：" + safe(context.getPlatform()) + "\n"
                // 追加风格偏好。
                + "风格偏好：" + safe(context.getStylePreference());
    }

    /**
     * buildRagContextText（构建检索上下文文本）用于读取 RAG（检索增强生成）最终注入文本。
     *
     * @param context 智能体上下文
     * @return RAG 上下文文本
     */
    private String buildRagContextText(AgentContext context) {
        String compressedText = compressedText(context, "COMPRESSED_RAG_CONTEXT");
        if (compressedText != null) {
            return compressedText;
        }
        // 读取知识检索结果。
        Object value = context.getArtifact("KNOWLEDGE_RETRIEVAL_RESULT");
        // 类型不匹配时返回空。
        if (!(value instanceof KnowledgeRetrievalResult result)) {
            // 返回空文本。
            return "";
        }
        // 返回最终注入 Prompt（提示词）的检索上下文。
        return safe(result.getPromptContext());
    }

    /**
     * buildMemoryContextText（构建记忆上下文文本）用于读取真实渲染后的记忆提示词片段。
     *
     * @param context 智能体上下文
     * @return 记忆上下文文本
     */
    private String buildMemoryContextText(AgentContext context) {
        String compressedText = compressedText(context, "COMPRESSED_MEMORY_CONTEXT");
        if (compressedText != null) {
            return compressedText;
        }
        // 使用记忆提示词渲染器生成文本，保持和真实 Prompt（提示词）注入方式一致。
        return memoryPromptRenderer.render(context.getMemoryContext());
    }

    /**
     * buildImageSummaryText（构建图片摘要文本）用于读取图片理解摘要。
     *
     * @param context 智能体上下文
     * @return 图片摘要文本
     */
    private String buildImageSummaryText(AgentContext context) {
        String compressedText = compressedText(context, "COMPRESSED_IMAGE_SUMMARY");
        if (compressedText != null) {
            return compressedText;
        }
        // 读取素材摘要。
        Object value = context.getArtifact("MATERIAL_SUMMARY");
        // 类型不匹配时返回空。
        if (!(value instanceof MaterialSummary summary)) {
            // 返回空文本。
            return "";
        }
        // 返回摘要文本。
        return safe(summary.getSummaryText());
    }

    /**
     * buildVideoSummaryText（构建视频摘要文本）用于读取视频理解摘要。
     *
     * @param context 智能体上下文
     * @return 视频摘要文本
     */
    private String buildVideoSummaryText(AgentContext context) {
        String compressedText = compressedText(context, "COMPRESSED_VIDEO_SUMMARY");
        if (compressedText != null) {
            return compressedText;
        }
        // 读取视频理解上下文。
        Object value = context.getArtifact("VIDEO_UNDERSTANDING_CREATION_CONTEXT");
        // 类型不匹配时返回空。
        if (!(value instanceof VideoUnderstandingCreationContext videoContext)) {
            // 返回空文本。
            return "";
        }
        // 返回摘要文本。
        return safe(videoContext.getSummaryText());
    }

    /**
     * buildOutputSchemaText（构建输出结构文本）用于粗略估算系统提示词里的输出结构约束。
     *
     * @param renderedPrompt 已渲染创作方案提示词
     * @return 输出结构约束文本
     */
    private String buildOutputSchemaText(RenderedCreationPlanPrompt renderedPrompt) {
        // 读取系统提示词。
        String systemPrompt = safe(renderedPrompt.getSystemPrompt());
        // 查找 JSON（对象表示格式）关键字。
        int index = systemPrompt.indexOf("JSON");
        // 找不到时返回空文本。
        if (index < 0) {
            // 返回空文本。
            return "";
        }
        // 从 JSON 关键字附近截取到末尾，作为输出结构约束近似片段。
        return systemPrompt.substring(index);
    }

    /**
     * safe（安全文本）用于把空引用转换为空字符串。
     *
     * @param value 原始文本
     * @return 安全文本
     */
    /**
     * compressedText（压缩覆盖文本）用于让估算片段和最终渲染片段保持一致。
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

    private String safe(String value) {
        // 空引用返回空字符串。
        if (value == null) {
            // 返回空字符串。
            return "";
        }
        // 返回原文本。
        return value;
    }
}
