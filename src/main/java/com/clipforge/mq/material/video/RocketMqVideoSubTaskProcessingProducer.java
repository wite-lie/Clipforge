package com.clipforge.mq.material.video;

import com.clipforge.mq.common.AbstractRocketMqSlowTaskProducer;
import com.clipforge.mq.common.SlowTaskBizType;
import com.clipforge.mq.common.SlowTaskMessageType;
import com.clipforge.mq.common.SlowTaskSendCommand;
import com.clipforge.mq.common.SlowTaskTriggerType;
import com.clipforge.mq.material.common.MaterialAsyncProcessProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RocketMqVideoSubTaskProcessingProducer（RocketMQ 视频子任务处理消息生产者）。
 *
 * <p>该生产者复用优化项 14 的 RocketMQ（消息队列）慢任务模板，统一包装视频子任务消息。
 */
@Component
@Profile("rocketmq")
@Primary
@ConditionalOnProperty(prefix = "clipforge.material.async-process", name = "enabled", havingValue = "true")
public class RocketMqVideoSubTaskProcessingProducer extends AbstractRocketMqSlowTaskProducer<VideoSubTaskProcessingMessage> implements VideoSubTaskProcessingProducer {

    /** properties（素材异步处理配置）用于读取视频子任务 Topic（主题）。 */
    private final MaterialAsyncProcessProperties properties;

    /**
     * RocketMqVideoSubTaskProcessingProducer（RocketMQ 视频子任务处理消息生产者）构造函数。
     *
     * @param rocketMQTemplate RocketMQ（消息队列）消息模板
     * @param properties 素材异步处理配置
     */
    public RocketMqVideoSubTaskProcessingProducer(RocketMQTemplate rocketMQTemplate, MaterialAsyncProcessProperties properties) {
        // 调用公共发送模板构造函数。
        super(rocketMQTemplate);
        // 保存素材异步处理配置。
        this.properties = properties;
    }

    /**
     * send（发送）用于投递视频子任务处理消息。
     *
     * @param message 视频子任务处理消息
     */
    @Override
    public void send(VideoSubTaskProcessingMessage message) {
        // 调用公共模板发送，模板会统一包装 Envelope（消息信封）、设置 Keys（消息键）、打印日志和处理异常。
        sendMessage(message);
    }

    /**
     * buildSendCommand（构造发送命令）用于声明视频子任务消息的 RocketMQ（消息队列）差异化发送参数。
     *
     * @param message 视频子任务处理消息
     * @return 慢任务发送命令
     */
    @Override
    protected SlowTaskSendCommand buildSendCommand(VideoSubTaskProcessingMessage message) {
        // 构造视频子任务慢任务发送命令。
        return SlowTaskSendCommand.builder()
                .eventName("视频子任务处理")
                .topic(properties.getVideoSubTaskTopic())
                .tag(message == null ? SlowTaskMessageType.VIDEO_PROCESSING.name() : message.getSubTaskType())
                .keys(message == null ? null : message.getProcessTaskId())
                .messageId(message == null ? null : message.getMessageId())
                .bizType(SlowTaskBizType.MATERIAL)
                .messageType(SlowTaskMessageType.VIDEO_PROCESSING)
                .triggerType(SlowTaskTriggerType.UPLOAD_AUTO)
                .retryCount(0)
                .sentTimeoutMillis(3000L)
                .build();
    }
}
