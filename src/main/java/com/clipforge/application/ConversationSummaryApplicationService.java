package com.clipforge.application;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.ConversationMessageEntity;
import com.clipforge.infra.persistence.entity.ConversationSummaryEntity;
import com.clipforge.infra.persistence.enums.ConversationSummaryStatus;
import com.clipforge.infra.persistence.repository.ConversationMessageRepository;
import com.clipforge.infra.persistence.repository.ConversationSummaryRepository;
import com.clipforge.memory.summary.conversation.ConversationSummaryGenerateRequest;
import com.clipforge.memory.summary.conversation.ConversationSummaryGenerateResult;
import com.clipforge.memory.summary.conversation.ConversationSummaryGenerator;
import com.clipforge.memory.summary.conversation.ConversationSummaryTriggerDecision;
import com.clipforge.memory.summary.conversation.ConversationSummaryTriggerPolicy;
import com.clipforge.memory.summary.conversation.ConversationSummaryTriggerRequest;
import com.clipforge.model.chat.ChatModelProperties;
import com.clipforge.model.chat.ChatModelResponse;
import com.clipforge.model.usage.ModelCallRecord;
import com.clipforge.model.usage.ModelUsageRecorder;
import com.clipforge.prompt.token.PromptTokenEstimator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * ConversationSummaryApplicationService（会话滚动摘要应用服务）。
 *
 * <p>该服务在创作前刷新 ConversationSummary（会话滚动摘要），让 MemoryReader（记忆读取器）读取到最新短期记忆。
 */
@Service
@RequiredArgsConstructor
public class ConversationSummaryApplicationService {

    /** conversationMessageRepositoryProvider（会话消息仓储提供器）用于读取会话消息。 */
    private final ObjectProvider<ConversationMessageRepository> conversationMessageRepositoryProvider;

    /** conversationSummaryRepositoryProvider（会话摘要仓储提供器）用于读取和写入滚动摘要。 */
    private final ObjectProvider<ConversationSummaryRepository> conversationSummaryRepositoryProvider;

    /** triggerPolicy（触发策略）用于判断是否需要调用 LLM（大语言模型）摘要。 */
    private final ConversationSummaryTriggerPolicy triggerPolicy;

    /** summaryGenerator（摘要生成器）负责调用 LLM（大语言模型）。 */
    private final ConversationSummaryGenerator summaryGenerator;

    /** promptTokenEstimator（提示词词元估算器）用于估算摘要长度。 */
    private final PromptTokenEstimator promptTokenEstimator;

    /** modelUsageRecorder（模型用量记录器）用于记录摘要模型调用成本。 */
    private final ModelUsageRecorder modelUsageRecorder;

    /** chatModelProperties（聊天模型配置）用于失败用量记录中的供应商和模型名兜底。 */
    private final ChatModelProperties chatModelProperties;

    /**
     * refreshBeforeCreation（创作前刷新）用于在读取 MemoryContext（记忆上下文）之前尝试更新会话滚动摘要。
     *
     * @param runId 运行编号
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param conversationId 会话编号
     * @return 触发决策
     */
    public ConversationSummaryTriggerDecision refreshBeforeCreation(String runId, String userId, String projectId, String conversationId) {
        if (!hasText(conversationId)) {
            return ConversationSummaryTriggerDecision.builder().shouldSummarize(false).reason("缺少 conversationId（会话编号）").summarizableMessages(List.of()).build();
        }
        ConversationMessageRepository messageRepository = requiredMessageRepository();
        ConversationSummaryRepository summaryRepository = requiredSummaryRepository();
        List<ConversationMessageEntity> messages = messageRepository.listByConversationId(conversationId);
        ConversationSummaryEntity existingSummary = summaryRepository.findActiveByConversationId(conversationId).orElse(null);
        ConversationSummaryTriggerDecision decision = triggerPolicy.decide(ConversationSummaryTriggerRequest.builder()
                .conversationId(conversationId)
                .existingSummary(existingSummary)
                .messages(messages)
                .build());
        if (!decision.isShouldSummarize()) {
            return decision;
        }
        long startedAt = System.currentTimeMillis();
        try {
            ConversationSummaryGenerateResult generateResult = summaryGenerator.generate(ConversationSummaryGenerateRequest.builder()
                    .runId(runId)
                    .conversationId(conversationId)
                    .projectId(projectId)
                    .userId(userId)
                    .existingSummary(existingSummary)
                    .messagesToSummarize(decision.getSummarizableMessages())
                    .build());
            ConversationMessageEntity lastMessage = decision.getSummarizableMessages().get(decision.getSummarizableMessages().size() - 1);
            summaryRepository.upsertSummary(ConversationSummaryEntity.builder()
                    .id(newId("summary_pk"))
                    .summaryId(existingSummary == null ? newId("summary") : existingSummary.getSummaryId())
                    .conversationId(conversationId)
                    .projectId(projectId)
                    .userId(userId)
                    .summaryContent(generateResult.getSummaryContent())
                    .summarizedUntilMessageId(lastMessage.getMessageId())
                    .summarizedUntilTime(lastMessage.getCreateTime() == null ? LocalDateTime.now() : lastMessage.getCreateTime())
                    .sourceMessageCount((existingSummary == null || existingSummary.getSourceMessageCount() == null ? 0 : existingSummary.getSourceMessageCount())
                            + decision.getSummarizableMessages().size())
                    .summaryVersion(existingSummary == null || existingSummary.getSummaryVersion() == null ? 1 : existingSummary.getSummaryVersion())
                    .tokenEstimate(promptTokenEstimator.estimateText(generateResult.getSummaryContent()))
                    .status(ConversationSummaryStatus.ACTIVE.value())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .deleted(0)
                    .build());
            recordUsage(runId, projectId, conversationId, generateResult.getModelResponse(), System.currentTimeMillis() - startedAt, true, null);
            return decision;
        } catch (RuntimeException exception) {
            recordUsage(runId, projectId, conversationId, null, System.currentTimeMillis() - startedAt, false, exception);
            throw exception;
        }
    }

    /**
     * recordUsage（记录用量）用于保存会话摘要模型调用成本。
     */
    private void recordUsage(String runId, String projectId, String conversationId, ChatModelResponse response, long latencyMs, boolean success, RuntimeException exception) {
        modelUsageRecorder.record(ModelCallRecord.builder()
                .runId(runId)
                .projectId(projectId)
                .conversationId(conversationId)
                .provider(response == null ? chatModelProperties.getProvider() : response.getProvider())
                .modelName(response == null ? chatModelProperties.getModelName() : response.getModelName())
                .purpose("CONVERSATION_SUMMARY_GENERATION")
                .promptTokens(response == null ? null : response.getPromptTokens())
                .completionTokens(response == null ? null : response.getCompletionTokens())
                .totalTokens(response == null ? null : response.getTotalTokens())
                .latencyMs(latencyMs)
                .success(success)
                .errorCode(exception == null ? null : exception.getClass().getSimpleName())
                .errorMessage(exception == null ? null : exception.getMessage())
                .build());
    }

    /**
     * requiredMessageRepository（获取必需会话消息仓储）用于处理未启用数据库的场景。
     */
    private ConversationMessageRepository requiredMessageRepository() {
        ConversationMessageRepository repository = conversationMessageRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "会话滚动摘要需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    /**
     * requiredSummaryRepository（获取必需会话摘要仓储）用于处理未启用数据库的场景。
     */
    private ConversationSummaryRepository requiredSummaryRepository() {
        ConversationSummaryRepository repository = conversationSummaryRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "会话滚动摘要需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    /**
     * newId（新编号）用于生成阶段内唯一编号。
     */
    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * hasText（是否有文本）用于判断字符串不为空。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
