package com.clipforge.mq.material.image;

import com.clipforge.application.ImageUnderstandingProcessApplicationService;
import com.clipforge.mq.common.SlowTaskMessageEnvelope;
import com.clipforge.mq.material.common.MaterialConsumerConcurrency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RocketMqImageUnderstandingConsumer（RocketMQ 图片理解消息消费者）。
 */
@Component
@Profile("rocketmq")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "clipforge.material.async-process", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "${clipforge.material.async-process.image-topic}",
        consumerGroup = "${clipforge.material.async-process.image-consumer-group}",
        consumeThreadNumber = MaterialConsumerConcurrency.IMAGE_CONSUME_THREAD_NUMBER,
        consumeThreadMax = MaterialConsumerConcurrency.IMAGE_CONSUME_THREAD_MAX
)
public class RocketMqImageUnderstandingConsumer implements RocketMQListener<SlowTaskMessageEnvelope<ImageUnderstandingMessage>> {

    private final ImageUnderstandingProcessApplicationService imageUnderstandingProcessApplicationService;

    @Override
    public void onMessage(SlowTaskMessageEnvelope<ImageUnderstandingMessage> envelope) {
        ImageUnderstandingMessage message = envelope == null ? null : envelope.getPayload();
        if (message == null) {
            return;
        }
        try {
            imageUnderstandingProcessApplicationService.process(message.getProcessTaskId(), message.getInstruction());
        } catch (RuntimeException exception) {
            log.error("RocketMQ 图片理解消息消费失败，processTaskId={}，materialId={}，异常类型={}，原因={}",
                    message.getProcessTaskId(), message.getMaterialId(), exception.getClass().getName(), exception.getMessage(), exception);
        }
    }
}
