package com.clipforge.messaging.rocketmq.material.common;

/**
 * MaterialConsumerConcurrency（素材消费者并发配置）集中声明 RocketMQ（消息队列）消费者线程数。
 *
 * <p>RocketMQMessageListener（RocketMQ 消息监听注解）的消费线程字段是 int（整数），不能直接使用 YAML（配置文件）
 * 占位符。因此第一版先用编译期常量集中管理，避免线程数散落在多个消费者注解里。
 */
public final class MaterialConsumerConcurrency {

    /** IMAGE_CONSUME_THREAD_NUMBER（图片消费核心线程数）表示图片理解消费者启动后的基础消费线程数。 */
    public static final int IMAGE_CONSUME_THREAD_NUMBER = 4;

    /** IMAGE_CONSUME_THREAD_MAX（图片消费最大线程数）表示图片理解消费者高峰期最大消费线程数。 */
    public static final int IMAGE_CONSUME_THREAD_MAX = 8;

    /** VIDEO_CONSUME_THREAD_NUMBER（视频消费核心线程数）表示视频处理消费者启动后的基础消费线程数。 */
    public static final int VIDEO_CONSUME_THREAD_NUMBER = 2;

    /** VIDEO_CONSUME_THREAD_MAX（视频消费最大线程数）表示视频处理消费者高峰期最大消费线程数。 */
    public static final int VIDEO_CONSUME_THREAD_MAX = 4;

    /**
     * 私有构造函数避免工具类被实例化。
     */
    private MaterialConsumerConcurrency() {
    }
}
