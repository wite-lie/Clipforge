package com.clipforge.mq.material.video;

import org.springframework.stereotype.Component;

/**
 * NoOpVideoSubTaskProcessingProducer（空视频子任务消息生产者）。
 *
 * <p>未启用 RocketMQ（消息队列）时保持应用可启动，不真正投递消息。
 */
@Component
public class NoOpVideoSubTaskProcessingProducer implements VideoSubTaskProcessingProducer {

    @Override
    public void send(VideoSubTaskProcessingMessage message) {
        // 未启用 RocketMQ 时安静跳过，避免本地测试产生大量无意义控制台输出。
    }
}
