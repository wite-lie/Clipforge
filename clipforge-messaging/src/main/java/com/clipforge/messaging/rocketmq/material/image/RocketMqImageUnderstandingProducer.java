package com.clipforge.messaging.rocketmq.material.image;

import com.clipforge.messaging.rocketmq.common.AbstractRocketMqSlowTaskProducer;
import com.clipforge.messaging.rocketmq.common.SlowTaskBizType;
import com.clipforge.messaging.rocketmq.common.SlowTaskMessageType;
import com.clipforge.messaging.rocketmq.common.SlowTaskSendCommand;
import com.clipforge.messaging.rocketmq.common.SlowTaskTriggerType;
import com.clipforge.messaging.rocketmq.material.common.MaterialAsyncProcessProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RocketMqImageUnderstandingProducer（RocketMQ 图片理解消息生产者）。
 *
 * <p>该实现只在 rocketmq Profile（消息队列配置环境）且素材异步处理开关启用时注册。
 * 应用层仍然发送 ImageUnderstandingMessage（图片理解消息），真正进入 RocketMQ（消息队列）前由公共模板包装为
 * SlowTaskMessageEnvelope（慢任务消息信封）。
 */
@Component
@Profile("rocketmq")
@Primary
@ConditionalOnProperty(prefix = "clipforge.material.async-process", name = "enabled", havingValue = "true")
public class RocketMqImageUnderstandingProducer extends AbstractRocketMqSlowTaskProducer<ImageUnderstandingMessage> implements ImageUnderstandingProducer {

    /** properties（素材异步处理配置）用于读取图片理解 Topic（主题）。 */
    private final MaterialAsyncProcessProperties properties;

    /**
     * RocketMqImageUnderstandingProducer（RocketMQ 图片理解消息生产者）构造函数。
     *
     * @param rocketMQTemplate RocketMQ（消息队列）消息模板
     * @param properties 素材异步处理配置
     */
    public RocketMqImageUnderstandingProducer(RocketMQTemplate rocketMQTemplate, MaterialAsyncProcessProperties properties) {
        // 把真实发送能力交给公共模板。
        super(rocketMQTemplate);
        // 保存素材异步处理配置。
        this.properties = properties;
    }

    /**
     * send（发送）用于同步投递图片理解消息，发送失败会由公共模板包装为业务异常。
     *
     * @param message 图片理解消息
     */
    @Override
    public void send(ImageUnderstandingMessage message) {
        // 调用公共模板发送，模板会统一包装 Envelope（消息信封）、设置 Keys（消息键）、打印日志和处理异常。
        sendMessage(message);
    }

    /**
     * buildSendCommand（构造发送命令）用于声明图片理解消息的 RocketMQ（消息队列）差异化发送参数。
     *
     * @param message 图片理解消息
     * @return 慢任务发送命令
     */
    @Override
    protected SlowTaskSendCommand buildSendCommand(ImageUnderstandingMessage message) {
        // 构造图片理解慢任务发送命令。
        return SlowTaskSendCommand.builder()
                .eventName("图片素材理解")
                .topic(properties.getImageTopic())
                .tag(SlowTaskMessageType.IMAGE_UNDERSTANDING.name())
                .keys(message == null ? null : message.getProcessTaskId())
                .messageId(message == null ? null : message.getMessageId())
                .bizType(SlowTaskBizType.MATERIAL)
                .messageType(SlowTaskMessageType.IMAGE_UNDERSTANDING)
                .triggerType(SlowTaskTriggerType.UPLOAD_AUTO)
                .retryCount(0)
                .sentTimeoutMillis(3000L)
                .build();
    }
}
