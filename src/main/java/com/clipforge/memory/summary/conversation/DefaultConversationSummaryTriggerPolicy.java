package com.clipforge.memory.summary.conversation;

import com.clipforge.infra.persistence.entity.ConversationMessageEntity;
import com.clipforge.prompt.token.PromptTokenEstimator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DefaultConversationSummaryTriggerPolicy（默认会话摘要触发策略）。
 *
 * <p>该策略保留最近若干条消息原文，只摘要更早且尚未摘要过的消息。
 */
@Component
@RequiredArgsConstructor
public class DefaultConversationSummaryTriggerPolicy implements ConversationSummaryTriggerPolicy {

    /** properties（配置）用于读取最近窗口和触发阈值。 */
    private final ConversationSummaryProperties properties;

    /** promptTokenEstimator（提示词词元估算器）用于本地粗略估算会话历史长度。 */
    private final PromptTokenEstimator promptTokenEstimator;

    @Override
    public ConversationSummaryTriggerDecision decide(ConversationSummaryTriggerRequest request) {
        if (!properties.isEnabled()) {
            return skip("会话滚动摘要未启用", List.of(), 0, 0);
        }
        if (request == null || !hasText(request.getConversationId())) {
            return skip("缺少 conversationId（会话编号）", List.of(), 0, 0);
        }
        List<ConversationMessageEntity> messages = request.getMessages() == null ? List.of() : request.getMessages();
        if (messages.size() <= properties.getRecentMessageCount()) {
            return skip("消息数量未超过最近窗口", List.of(), estimateMessages(messages), 0);
        }
        List<ConversationMessageEntity> candidates = summarizableCandidates(request, messages);
        int totalHistoryTokens = estimateMessages(messages);
        int unsummarizedChars = candidates.stream()
                .map(ConversationMessageEntity::getContent)
                .filter(this::hasText)
                .mapToInt(String::length)
                .sum();
        if (candidates.size() < properties.getMinSummarizableMessageCount()) {
            return skip("可摘要旧消息数量不足", candidates, totalHistoryTokens, unsummarizedChars);
        }
        if (totalHistoryTokens >= properties.getMaxConversationHistoryTokens()) {
            return summarize("完整会话历史估算 Token 已超过阈值", candidates, totalHistoryTokens, unsummarizedChars);
        }
        if (unsummarizedChars >= properties.getMaxUnsummarizedChars()) {
            return summarize("未摘要旧消息字符数已超过阈值", candidates, totalHistoryTokens, unsummarizedChars);
        }
        return skip("未达到摘要触发阈值", candidates, totalHistoryTokens, unsummarizedChars);
    }

    /**
     * summarizableCandidates（可摘要候选）用于筛出已摘要边界之后、最近窗口之前的消息。
     */
    private List<ConversationMessageEntity> summarizableCandidates(ConversationSummaryTriggerRequest request, List<ConversationMessageEntity> messages) {
        int recentStartIndex = Math.max(0, messages.size() - properties.getRecentMessageCount());
        LocalDateTime summarizedUntilTime = request.getExistingSummary() == null ? null : request.getExistingSummary().getSummarizedUntilTime();
        String summarizedUntilMessageId = request.getExistingSummary() == null ? null : request.getExistingSummary().getSummarizedUntilMessageId();
        List<ConversationMessageEntity> candidates = new ArrayList<>();
        for (int index = 0; index < recentStartIndex; index++) {
            ConversationMessageEntity message = messages.get(index);
            if (alreadySummarized(message, summarizedUntilTime, summarizedUntilMessageId)) {
                continue;
            }
            candidates.add(message);
        }
        return candidates;
    }

    /**
     * alreadySummarized（是否已摘要）用于避免旧消息被重复压缩。
     */
    private boolean alreadySummarized(ConversationMessageEntity message, LocalDateTime summarizedUntilTime, String summarizedUntilMessageId) {
        if (message == null) {
            return true;
        }
        if (hasText(summarizedUntilMessageId) && summarizedUntilMessageId.equals(message.getMessageId())) {
            return true;
        }
        return summarizedUntilTime != null && message.getCreateTime() != null && !message.getCreateTime().isAfter(summarizedUntilTime);
    }

    /**
     * estimateMessages（估算消息）用于估算会话消息整体 Token（词元）。
     */
    private int estimateMessages(List<ConversationMessageEntity> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        return promptTokenEstimator.estimateText(renderMessages(messages));
    }

    /**
     * renderMessages（渲染消息）用于把消息列表转换为可估算文本。
     */
    private String renderMessages(List<ConversationMessageEntity> messages) {
        StringBuilder builder = new StringBuilder();
        for (ConversationMessageEntity message : messages) {
            builder.append(message.getRole()).append(": ")
                    .append(message.getContent() == null ? "" : message.getContent())
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    /**
     * summarize（摘要）用于构造触发决策。
     */
    private ConversationSummaryTriggerDecision summarize(String reason, List<ConversationMessageEntity> candidates, int totalHistoryTokens, int unsummarizedChars) {
        return ConversationSummaryTriggerDecision.builder()
                .shouldSummarize(true)
                .reason(reason)
                .summarizableMessages(candidates)
                .totalHistoryTokens(totalHistoryTokens)
                .unsummarizedChars(unsummarizedChars)
                .build();
    }

    /**
     * skip（跳过）用于构造不触发决策。
     */
    private ConversationSummaryTriggerDecision skip(String reason, List<ConversationMessageEntity> candidates, int totalHistoryTokens, int unsummarizedChars) {
        return ConversationSummaryTriggerDecision.builder()
                .shouldSummarize(false)
                .reason(reason)
                .summarizableMessages(candidates)
                .totalHistoryTokens(totalHistoryTokens)
                .unsummarizedChars(unsummarizedChars)
                .build();
    }

    /**
     * hasText（是否有文本）用于判断字符串不为空。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
