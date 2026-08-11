package com.clipforge.agent.intent;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * ConversationIntentPromptRenderer（会话意图提示词渲染器）。
 *
 * <p>该组件负责从 Prompt（提示词）文件读取模板，并把本轮消息、短历史和结构化上下文字段
 * 渲染为 LLM（大语言模型）分类请求。
 */
@Component
@RequiredArgsConstructor
public class ConversationIntentPromptRenderer {

    /** properties（配置）用于读取 Prompt（提示词）路径和历史数量限制。 */
    private final ConversationIntentProperties properties;

    /** resourceLoader（资源加载器）用于读取 classpath（类路径）下的 Prompt（提示词）文件。 */
    private final ResourceLoader resourceLoader;

    /**
     * render（渲染）用于生成意图分类模型的 system/user（系统/用户）提示词。
     */
    public RenderedIntentPrompt render(ConversationIntentRequest request) {
        String systemPrompt = readResource(properties.getSystemPath());
        String userPrompt = readResource(properties.getUserPath())
                .replace("{{message}}", safe(request == null ? null : request.getMessage()))
                .replace("{{userId}}", safe(request == null ? null : request.getUserId()))
                .replace("{{projectId}}", safe(request == null ? null : request.getProjectId()))
                .replace("{{conversationId}}", safe(request == null ? null : request.getConversationId()))
                .replace("{{runId}}", safe(request == null ? null : request.getRunId()))
                .replace("{{platform}}", safe(request == null ? null : request.getPlatform()))
                .replace("{{stylePreference}}", safe(request == null ? null : request.getStylePreference()))
                .replace("{{materialIds}}", formatList(request == null ? null : request.getMaterialIds()))
                .replace("{{processTaskIds}}", formatList(request == null ? null : request.getProcessTaskIds()))
                .replace("{{knowledgeBaseIds}}", formatList(request == null ? null : request.getKnowledgeBaseIds()))
                .replace("{{recentMessages}}", formatRecentMessages(request == null ? null : request.getRecentMessages()));
        return RenderedIntentPrompt.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .build();
    }

    /**
     * readResource（读取资源）用于按 UTF-8（统一码转换格式）读取 Prompt（提示词）文件。
     */
    private String readResource(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取会话意图 Prompt 文件失败：" + path);
        }
    }

    /**
     * formatRecentMessages（格式化最近消息）用于给模型提供短上下文，不传完整长会话。
     */
    private String formatRecentMessages(List<String> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return "无";
        }
        return recentMessages.stream()
                .filter(this::hasText)
                .limit(properties.getMaxRecentMessages())
                .map(this::safe)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("无");
    }

    /**
     * formatList（格式化列表）用于稳定展示编号字段。
     */
    private String formatList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "无";
        }
        return values.stream()
                .filter(this::hasText)
                .map(String::strip)
                .reduce((left, right) -> left + ", " + right)
                .orElse("无");
    }

    /**
     * safe（安全文本）用于去除首尾空白并限制单项长度。
     */
    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "无";
        }
        String trimmed = value.strip();
        return trimmed.length() > 800 ? trimmed.substring(0, 800) + "...（已截断）" : trimmed;
    }

    /**
     * hasText（是否有文本）用于判断字符串是否包含有效内容。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
