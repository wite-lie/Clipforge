package com.clipforge.mq.material.video;

/**
 * VideoSubTaskProcessingProducer（视频子任务处理消息生产者）。
 *
 * <p>该接口屏蔽 RocketMQ（消息队列）发送细节，编排器只提交视频子任务业务消息。
 */
public interface VideoSubTaskProcessingProducer {

    /**
     * send（发送）用于投递视频子任务处理消息。
     *
     * @param message 视频子任务处理消息
     */
    void send(VideoSubTaskProcessingMessage message);
}
