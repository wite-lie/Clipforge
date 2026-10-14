package com.clipforge.messaging.rocketmq.material.image;

/**
 * ImageUnderstandingProducer（图片理解消息生产者）。
 *
 * <p>该接口屏蔽 RocketMQ（消息队列）发送细节，应用层只提交图片理解业务消息。
 */
public interface ImageUnderstandingProducer {

    /**
     * send（发送）用于投递图片理解消息。
     *
     * @param message 图片理解消息
     */
    void send(ImageUnderstandingMessage message);
}
