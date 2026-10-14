package com.clipforge.messaging.rocketmq.material.video;

/**
 * VideoProcessingProducer（视频处理消息生产者）。
 *
 * <p>该接口屏蔽 RocketMQ（消息队列）发送细节，应用层只提交视频处理业务消息。
 */
public interface VideoProcessingProducer {

    /**
     * send（发送）用于投递视频处理消息。
     *
     * @param message 视频处理消息
     */
    void send(VideoProcessingMessage message);
}
