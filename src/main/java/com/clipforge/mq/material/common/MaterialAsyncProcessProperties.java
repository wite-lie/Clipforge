package com.clipforge.mq.material.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MaterialAsyncProcessProperties（素材异步处理配置）。
 *
 * <p>该配置集中管理图片理解和视频处理的 RocketMQ（消息队列）主题、消费组和自动触发开关。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.material.async-process")
public class MaterialAsyncProcessProperties {

    /** enabled（是否启用）表示是否注册 RocketMQ（消息队列）素材处理生产者和消费者。 */
    private boolean enabled = false;

    /** autoDispatchOnUpload（上传后是否自动投递）表示图片或视频上传成功后是否自动发送处理消息。 */
    private boolean autoDispatchOnUpload = false;

    /** imageTopic（图片主题）表示图片理解任务投递到哪个 RocketMQ（消息队列）主题。 */
    private String imageTopic = "clipforge-material-image-understanding-topic";

    /** videoTopic（视频主题）表示视频处理任务投递到哪个 RocketMQ（消息队列）主题。 */
    private String videoTopic = "clipforge-material-video-processing-topic";

    /** videoSubTaskTopic（视频子任务主题）表示视频抽帧图像分支、音频分支和汇总子任务投递到哪个 RocketMQ（消息队列）主题。 */
    private String videoSubTaskTopic = "clipforge-material-video-sub-task-topic";

    /** imageConsumerGroup（图片消费组）表示消费图片理解消息的消费者组。 */
    private String imageConsumerGroup = "clipforge-material-image-understanding-consumer-group";

    /** videoConsumerGroup（视频消费组）表示消费视频处理消息的消费者组。 */
    private String videoConsumerGroup = "clipforge-material-video-processing-consumer-group";

    /** videoSubTaskConsumerGroup（视频子任务消费者组）表示消费视频子任务消息的消费者组。 */
    private String videoSubTaskConsumerGroup = "clipforge-material-video-sub-task-consumer-group";

    /** defaultImageInstruction（默认图片理解指令）用于上传后自动理解图片时构造稳定任务目标。 */
    private String defaultImageInstruction = "请面向短视频创作规划理解这张图片，重点识别主体、场景、视觉风格、可用于分镜的画面元素、图片文字和潜在风险。";

    /** defaultVideoInstruction（默认视频理解指令）用于上传后自动处理视频时构造稳定任务目标。 */
    private String defaultVideoInstruction = "请面向短视频创作规划理解该视频，重点总结关键画面、音频线索、可能的高光片段、适合创作的卖点和风险提示。";
}
