package com.clipforge.messaging.rocketmq.material.video;

import com.clipforge.application.VideoProcessTaskOrchestrator;
import com.clipforge.messaging.rocketmq.common.SlowTaskMessageEnvelope;
import com.clipforge.messaging.rocketmq.material.common.MaterialConsumerConcurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RocketMqVideoProcessingConsumer（RocketMQ 视频处理消息消费者）。
 */
@Component
@Profile("rocketmq")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "clipforge.material.async-process", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "${clipforge.material.async-process.video-topic}",
        consumerGroup = "${clipforge.material.async-process.video-consumer-group}",
        consumeThreadNumber = MaterialConsumerConcurrency.VIDEO_CONSUME_THREAD_NUMBER,
        consumeThreadMax = MaterialConsumerConcurrency.VIDEO_CONSUME_THREAD_MAX
)
public class RocketMqVideoProcessingConsumer implements RocketMQListener<SlowTaskMessageEnvelope<VideoProcessingMessage>> {

    private final VideoProcessTaskOrchestrator videoProcessTaskOrchestrator;

    @Override
    public void onMessage(SlowTaskMessageEnvelope<VideoProcessingMessage> envelope) {
        VideoProcessingMessage message = envelope == null ? null : envelope.getPayload();
        if (message == null) {
            return;
        }
        try {
            videoProcessTaskOrchestrator.processParentTask(message.getProcessTaskId(), message.getInstruction());
        } catch (RuntimeException exception) {
            log.error("RocketMQ 视频处理消息消费失败，processTaskId={}，异常类型={}，原因={}",
                    message.getProcessTaskId(), exception.getClass().getName(), exception.getMessage(), exception);
        }
    }
}
