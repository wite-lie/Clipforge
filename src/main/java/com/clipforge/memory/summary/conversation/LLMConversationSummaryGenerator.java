package com.clipforge.memory.summary.conversation;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.ConversationMessageEntity;
import com.clipforge.model.chat.ChatMessage;
import com.clipforge.model.chat.ChatModelClient;
import com.clipforge.model.chat.ChatModelRequest;
import com.clipforge.model.chat.ChatModelResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * LLMConversationSummaryGenerator（大语言模型会话摘要生成器）。
 *
 * <p>该实现直接调用 ChatModelClient（聊天模型客户端）生成会话滚动摘要。
 */
@Component
@RequiredArgsConstructor
public class LLMConversationSummaryGenerator implements ConversationSummaryGenerator {

    /** chatModelClient（聊天模型客户端）用于调用真实或 Mock（模拟）聊天模型。 */
    private final ChatModelClient chatModelClient;

    /** properties（配置）用于读取摘要模型参数和 Prompt（提示词）路径。 */
    private final ConversationSummaryProperties properties;

    /** resourceLoader（资源加载器）用于读取 classpath（类路径）下的 Prompt（提示词）文件。 */
    private final ResourceLoader resourceLoader;

    @Override
    public ConversationSummaryGenerateResult generate(ConversationSummaryGenerateRequest request) {
        validateRequest(request);
        String systemPrompt = readResource(properties.getSystemPath());
        String userPrompt = renderUserPrompt(readResource(properties.getUserPath()), request);
        ChatModelResponse response = chatModelClient.chat(ChatModelRequest.builder()
                .runId(request.getRunId())
                .purpose("CONVERSATION_SUMMARY_GENERATION")
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxSummaryTokens())
                .messages(List.of(
                        ChatMessage.system(systemPrompt),
                        ChatMessage.user(userPrompt)
                ))
                .build());
        String summary = response == null ? null : response.getContent();
        if (summary == null || summary.isBlank()) {
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "会话滚动摘要模型返回为空");
        }
        return ConversationSummaryGenerateResult.builder()
                .summaryContent(summary.trim())
                .modelResponse(response)
                .build();
    }

    /**
     * renderUserPrompt（渲染用户提示词）用于把已有摘要和新增消息填入模板。
     */
    private String renderUserPrompt(String template, ConversationSummaryGenerateRequest request) {
        return template
                .replace("{{conversationId}}", safe(request.getConversationId()))
                .replace("{{projectId}}", safe(request.getProjectId()))
                .replace("{{userId}}", safe(request.getUserId()))
                .replace("{{existingSummary}}", request.getExistingSummary() == null ? "当前没有已有会话摘要。" : safe(request.getExistingSummary().getSummaryContent()))
                .replace("{{newMessages}}", renderMessages(request.getMessagesToSummarize()));
    }

    /**
     * renderMessages（渲染消息）用于把待摘要消息列表转为稳定文本。
     */
    private String renderMessages(List<ConversationMessageEntity> messages) {
        StringBuilder builder = new StringBuilder();
        for (ConversationMessageEntity message : messages) {
            builder.append("- messageId：").append(safe(message.getMessageId())).append(System.lineSeparator())
                    .append("  role：").append(safe(message.getRole())).append(System.lineSeparator())
                    .append("  time：").append(message.getCreateTime() == null ? "" : message.getCreateTime()).append(System.lineSeparator())
                    .append("  content：").append(safe(message.getContent())).append(System.lineSeparator());
        }
        return builder.toString();
    }

    /**
     * validateRequest（校验请求）用于避免向模型发送空摘要任务。
     */
    private void validateRequest(ConversationSummaryGenerateRequest request) {
        if (request == null || request.getMessagesToSummarize() == null || request.getMessagesToSummarize().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "会话摘要生成请求缺少待摘要消息");
        }
    }

    /**
     * readResource（读取资源）用于读取 Prompt（提示词）模板文件。
     */
    private String readResource(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_ERROR, "会话摘要提示词模板不存在：" + path);
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_ERROR, "会话摘要提示词模板读取失败：" + path, exception);
        }
    }

    /**
     * safe（安全文本）用于避免模板替换出现 null（空引用）。
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}
