package com.clipforge.mq.rag.urlsync;

import com.clipforge.infra.persistence.enums.KnowledgeDocumentProcessTriggerType;
import com.clipforge.mq.common.AbstractRocketMqSlowTaskProducer;
import com.clipforge.mq.common.SlowTaskBizType;
import com.clipforge.mq.common.SlowTaskMessageType;
import com.clipforge.mq.common.SlowTaskSendCommand;
import com.clipforge.mq.common.SlowTaskTriggerType;
import com.clipforge.rag.sync.KnowledgeDocumentUrlSyncProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RocketMqKnowledgeUrlSyncProducer（RocketMQ 知识库 URL 同步生产者）。
 */
@Component
@Profile("rocketmq")
@ConditionalOnProperty(prefix = "clipforge.rag.url-sync", name = "dispatch-to-mq", havingValue = "true")
public class RocketMqKnowledgeUrlSyncProducer extends AbstractRocketMqSlowTaskProducer<KnowledgeUrlSyncMessage> implements KnowledgeUrlSyncProducer {

    /** properties（URL 同步配置）用于读取 Topic（主题）等配置。 */
    private final KnowledgeDocumentUrlSyncProperties properties;

    public RocketMqKnowledgeUrlSyncProducer(RocketMQTemplate rocketMQTemplate, KnowledgeDocumentUrlSyncProperties properties) {
        super(rocketMQTemplate);
        this.properties = properties;
    }

    @Override
    public void send(KnowledgeUrlSyncMessage message) {
        sendMessage(message);
    }

    @Override
    protected SlowTaskSendCommand buildSendCommand(KnowledgeUrlSyncMessage message) {
        return SlowTaskSendCommand.builder()
                .eventName("RAG URL 同步")
                .topic(properties.getTopic())
                .tag(SlowTaskMessageType.RAG_URL_SYNC.name())
                .keys(message == null ? null : message.getScheduleId())
                .messageId(message == null ? null : message.getMessageId())
                .bizType(SlowTaskBizType.RAG)
                .messageType(SlowTaskMessageType.RAG_URL_SYNC)
                .triggerType(toSlowTaskTriggerType(message == null ? null : message.getTriggerType()))
                .retryCount(message == null || message.getRetryTimes() == null ? 0 : message.getRetryTimes())
                .sentTimeoutMillis(3000L)
                .build();
    }

    private SlowTaskTriggerType toSlowTaskTriggerType(KnowledgeDocumentProcessTriggerType triggerType) {
        if (triggerType == null) {
            return SlowTaskTriggerType.SCHEDULE;
        }
        return switch (triggerType) {
            case MANUAL -> SlowTaskTriggerType.MANUAL;
            case SCHEDULE -> SlowTaskTriggerType.SCHEDULE;
            case MQ_RETRY -> SlowTaskTriggerType.MQ_RETRY;
            case SYSTEM_SEED -> SlowTaskTriggerType.SYSTEM;
        };
    }
}
