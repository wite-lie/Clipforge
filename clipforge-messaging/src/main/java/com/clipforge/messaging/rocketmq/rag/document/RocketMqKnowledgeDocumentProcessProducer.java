package com.clipforge.messaging.rocketmq.rag.document;

import com.clipforge.messaging.rocketmq.common.AbstractRocketMqSlowTaskProducer;
import com.clipforge.messaging.rocketmq.common.SlowTaskBizType;
import com.clipforge.messaging.rocketmq.common.SlowTaskMessageType;
import com.clipforge.messaging.rocketmq.common.SlowTaskSendCommand;
import com.clipforge.messaging.rocketmq.common.SlowTaskTriggerType;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RocketMqKnowledgeDocumentProcessProducer（RocketMQ 知识文档处理消息生产者）。
 *
 * <p>该实现只在 rocketmq Profile（消息队列配置环境）且 RAG（检索增强生成）异步处理开关启用时注册。
 * 应用层仍然发送 KnowledgeDocumentProcessMessage（知识文档处理消息），真正进入 RocketMQ（消息队列）前由公共模板包装为
 * SlowTaskMessageEnvelope（慢任务消息信封）。
 */
@Component
@Profile("rocketmq")
@ConditionalOnProperty(prefix = "clipforge.rag.async-process", name = "enabled", havingValue = "true")
public class RocketMqKnowledgeDocumentProcessProducer extends AbstractRocketMqSlowTaskProducer<KnowledgeDocumentProcessMessage> implements KnowledgeDocumentProcessProducer {

    /** properties（知识文档异步处理配置）用于读取 Topic（主题）等配置。 */
    private final KnowledgeDocumentAsyncProcessProperties properties;

    /**
     * RocketMqKnowledgeDocumentProcessProducer（RocketMQ 知识文档处理消息生产者）构造函数。
     *
     * @param rocketMQTemplate RocketMQ（消息队列）消息模板
     * @param properties 知识文档异步处理配置
     */
    public RocketMqKnowledgeDocumentProcessProducer(RocketMQTemplate rocketMQTemplate, KnowledgeDocumentAsyncProcessProperties properties) {
        // 把真实发送能力交给公共模板。
        super(rocketMQTemplate);
        // 保存知识文档异步处理配置。
        this.properties = properties;
    }

    /**
     * send（发送）用于同步投递知识文档处理消息，发送失败会由公共模板包装为业务异常。
     *
     * @param message 知识文档处理消息
     */
    @Override
    public void send(KnowledgeDocumentProcessMessage message) {
        // 调用公共模板发送，模板会统一包装 Envelope（消息信封）、设置 Keys（消息键）、打印日志和处理异常。
        sendMessage(message);
    }

    /**
     * buildSendCommand（构造发送命令）用于声明知识文档处理消息的 RocketMQ（消息队列）差异化发送参数。
     *
     * @param message 知识文档处理消息
     * @return 慢任务发送命令
     */
    @Override
    protected SlowTaskSendCommand buildSendCommand(KnowledgeDocumentProcessMessage message) {
        // 构造知识文档处理慢任务发送命令。
        return SlowTaskSendCommand.builder()
                .eventName("知识文档处理")
                .topic(properties.getTopic())
                .tag(SlowTaskMessageType.KNOWLEDGE_DOCUMENT_PROCESS.name())
                .keys(message == null ? null : message.getProcessLogId())
                .messageId(message == null ? null : message.getMessageId())
                .bizType(SlowTaskBizType.RAG)
                .messageType(SlowTaskMessageType.KNOWLEDGE_DOCUMENT_PROCESS)
                .triggerType(toSlowTaskTriggerType(message == null ? null : message.getTriggerType()))
                .retryCount(message == null || message.getRetryTimes() == null ? 0 : message.getRetryTimes())
                .sentTimeoutMillis(3000L)
                .build();
    }

    /**
     * toSlowTaskTriggerType（转换慢任务触发类型）用于把 RAG（检索增强生成）内部触发枚举映射为统一 MQ（消息队列）触发枚举。
     *
     * @param triggerType 知识文档处理触发类型
     * @return 慢任务触发类型
     */
    private SlowTaskTriggerType toSlowTaskTriggerType(String triggerType) {
        if (triggerType == null || triggerType.isBlank()) {
            return SlowTaskTriggerType.MANUAL;
        }
        return switch (triggerType) {
            case "MANUAL" -> SlowTaskTriggerType.MANUAL;
            case "SCHEDULE" -> SlowTaskTriggerType.SCHEDULE;
            case "MQ_RETRY" -> SlowTaskTriggerType.MQ_RETRY;
            case "SYSTEM_SEED" -> SlowTaskTriggerType.SYSTEM;
            default -> SlowTaskTriggerType.SYSTEM;
        };
    }
}
