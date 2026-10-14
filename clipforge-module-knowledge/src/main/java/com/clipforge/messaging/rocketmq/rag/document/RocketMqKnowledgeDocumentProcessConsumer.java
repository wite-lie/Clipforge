package com.clipforge.messaging.rocketmq.rag.document;

import com.clipforge.knowledge.application.processing.KnowledgeDocumentProcessUseCase;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessLogStatus;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessPhase;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessTriggerType;
import com.clipforge.messaging.rocketmq.common.SlowTaskMessageEnvelope;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatus;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatusCache;
import com.clipforge.rag.recovery.KnowledgeDocumentProcessErrorType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RocketMqKnowledgeDocumentProcessConsumer（RocketMQ 知识文档处理消息消费者）。
 */
@Component
@Profile("rocketmq")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "clipforge.rag.async-process", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "${clipforge.rag.async-process.topic}",
        consumerGroup = "${clipforge.rag.async-process.consumer-group}"
)
public class RocketMqKnowledgeDocumentProcessConsumer implements RocketMQListener<SlowTaskMessageEnvelope<KnowledgeDocumentProcessMessage>> {

    private final KnowledgeDocumentProcessUseCase documentProcessUseCase;
    private final ObjectProvider<KnowledgeDocumentProcessStatusCache> statusCacheProvider;

    @Override
    public void onMessage(SlowTaskMessageEnvelope<KnowledgeDocumentProcessMessage> envelope) {
        KnowledgeDocumentProcessMessage message = envelope == null ? null : envelope.getPayload();
        if (message == null) {
            return;
        }
        try {
            documentProcessUseCase.processExistingLog(
                    message.getDocumentId(),
                    message.getProcessLogId(),
                    message.getRetryStage() == null ? null : message.getRetryStage().value());
        } catch (RuntimeException exception) {
            saveFailedStatus(message, exception);
            log.error("RocketMQ 知识文档处理消息消费失败，documentId={}，processLogId={}，异常类型={}，原因={}",
                    message.getDocumentId(), message.getProcessLogId(), exception.getClass().getName(), exception.getMessage(), exception);
        }
    }

    private void saveFailedStatus(KnowledgeDocumentProcessMessage message, RuntimeException exception) {
        KnowledgeDocumentProcessStatusCache statusCache = statusCacheProvider.getIfAvailable();
        if (statusCache == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            statusCache.save(KnowledgeDocumentProcessStatus.builder()
                    .documentId(message.getDocumentId())
                    .knowledgeBaseId(message.getKnowledgeBaseId())
                    .processLogId(message.getProcessLogId())
                    .status(KnowledgeDocumentProcessLogStatus.FAILED)
                    .phase(KnowledgeDocumentProcessPhase.PARSE_CHUNK)
                    .triggerType(parseTriggerType(message.getTriggerType()))
                    .progressPercent(100)
                    .message("知识文档异步处理消费失败")
                    .errorCode(KnowledgeDocumentProcessErrorType.MQ_CONSUME_FAILED.value())
                    .errorMessage(exception.getMessage())
                    .startedAt(message.getCreatedAt())
                    .updatedAt(now)
                    .endedAt(now)
                    .build());
        } catch (RuntimeException ignored) {
            // Redis（缓存）兜底状态写入失败不能再次触发消费异常。
        }
    }

    private KnowledgeDocumentProcessTriggerType parseTriggerType(String triggerType) {
        if (triggerType == null || triggerType.isBlank()) {
            return KnowledgeDocumentProcessTriggerType.MANUAL;
        }
        try {
            return KnowledgeDocumentProcessTriggerType.valueOf(triggerType);
        } catch (IllegalArgumentException ignored) {
            return KnowledgeDocumentProcessTriggerType.MANUAL;
        }
    }
}
