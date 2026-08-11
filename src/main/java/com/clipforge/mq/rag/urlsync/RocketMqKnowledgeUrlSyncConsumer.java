package com.clipforge.mq.rag.urlsync;

import com.clipforge.application.KnowledgeDocumentScheduleApplicationService;
import com.clipforge.mq.common.SlowTaskMessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RocketMqKnowledgeUrlSyncConsumer（RocketMQ 知识库 URL 同步消费者）。
 */
@Component
@Profile("rocketmq")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "clipforge.rag.url-sync", name = "dispatch-to-mq", havingValue = "true")
@RocketMQMessageListener(
        topic = "${clipforge.rag.url-sync.topic}",
        consumerGroup = "${clipforge.rag.url-sync.consumer-group}"
)
public class RocketMqKnowledgeUrlSyncConsumer implements RocketMQListener<SlowTaskMessageEnvelope<KnowledgeUrlSyncMessage>> {

    /** scheduleApplicationService（定时同步应用服务）负责执行 URL 同步。 */
    private final KnowledgeDocumentScheduleApplicationService scheduleApplicationService;

    @Override
    public void onMessage(SlowTaskMessageEnvelope<KnowledgeUrlSyncMessage> envelope) {
        KnowledgeUrlSyncMessage message = envelope == null ? null : envelope.getPayload();
        if (message == null || message.getScheduleId() == null || message.getScheduleId().isBlank()) {
            return;
        }
        try {
            scheduleApplicationService.executeLeasedSchedule(message.getScheduleId());
        } catch (RuntimeException exception) {
            log.error("RocketMQ RAG URL 同步消息消费失败，scheduleId={}，异常类型={}，原因={}",
                    message.getScheduleId(), exception.getClass().getName(), exception.getMessage(), exception);
        }
    }
}
