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
 * RocketMqVideoSubTaskProcessingConsumer（RocketMQ 视频子任务处理消息消费者）。
 */
@Component
@Profile("rocketmq")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "clipforge.material.async-process", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "${clipforge.material.async-process.video-sub-task-topic}",
        consumerGroup = "${clipforge.material.async-process.video-sub-task-consumer-group}",
        consumeThreadNumber = MaterialConsumerConcurrency.VIDEO_CONSUME_THREAD_NUMBER,
        consumeThreadMax = MaterialConsumerConcurrency.VIDEO_CONSUME_THREAD_MAX
)
public class RocketMqVideoSubTaskProcessingConsumer implements RocketMQListener<SlowTaskMessageEnvelope<VideoSubTaskProcessingMessage>> {

    private final VideoProcessTaskOrchestrator videoProcessTaskOrchestrator;

    @Override
    public void onMessage(SlowTaskMessageEnvelope<VideoSubTaskProcessingMessage> envelope) {
        VideoSubTaskProcessingMessage message = envelope == null ? null : envelope.getPayload();
        if (message == null) {
            return;
        }
        try {
            videoProcessTaskOrchestrator.processSubTask(message);
        } catch (RuntimeException exception) {
            log.error("RocketMQ 视频子任务消息消费失败，processTaskId={}，subTaskType={}，materialId={}，异常类型={}，原因={}",
                    message.getProcessTaskId(), message.getSubTaskType(), message.getMaterialId(), exception.getClass().getName(), exception.getMessage(), exception);
        }
    }
}
