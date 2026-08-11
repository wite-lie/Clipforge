package com.clipforge.mq.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SlowTaskSendCommand（慢任务发送命令）用于描述一次 RocketMQ（消息队列）发送所需的公共参数。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlowTaskSendCommand {

    /** eventName（事件名称）用于日志中展示人类可读的任务名称。 */
    private String eventName;

    /** topic（主题）表示 RocketMQ（消息队列）目标主题。 */
    private String topic;

    /** tag（标签）表示 RocketMQ（消息队列）目标标签，可为空。 */
    private String tag;

    /** keys（消息键）用于 RocketMQ（消息队列）检索、排查和幂等定位。 */
    private String keys;

    /** messageId（消息编号）表示业务侧生成的消息唯一标识。 */
    private String messageId;

    /** bizType（业务类型）表示消息所属业务域。 */
    private SlowTaskBizType bizType;

    /** messageType（消息类型）表示具体慢任务类型。 */
    private SlowTaskMessageType messageType;

    /** triggerType（触发类型）表示任务触发来源。 */
    private SlowTaskTriggerType triggerType;

    /** retryCount（重试次数）表示业务侧重试次数。 */
    private Integer retryCount;

    /** sentTimeoutMillis（发送超时毫秒数）表示同步发送最大等待时间。 */
    private Long sentTimeoutMillis;

    /** delayTimeMillis（延时投递毫秒时间点）为 null 时发送普通消息。 */
    private Long delayTimeMillis;
}
