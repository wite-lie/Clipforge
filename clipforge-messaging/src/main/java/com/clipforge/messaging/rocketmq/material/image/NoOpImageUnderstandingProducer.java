package com.clipforge.messaging.rocketmq.material.image;

import org.springframework.stereotype.Component;

/**
 * NoOpImageUnderstandingProducer（空图片理解消息生产者）。
 *
 * <p>未启用 RocketMQ（消息队列）时保持应用可启动，不真正投递消息。
 */
@Component
public class NoOpImageUnderstandingProducer implements ImageUnderstandingProducer {

    @Override
    public void send(ImageUnderstandingMessage message) {
        // 未启用 RocketMQ 时安静跳过，避免本地测试产生大量无意义控制台输出。
    }
}
