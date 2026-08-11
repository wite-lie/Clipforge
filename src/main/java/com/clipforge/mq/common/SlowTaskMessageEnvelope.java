package com.clipforge.mq.common;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SlowTaskMessageEnvelope（慢任务消息信封）用于统一包装真正进入 RocketMQ（消息队列）的业务消息。
 *
 * @param <T> payload（载荷）的业务消息类型
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlowTaskMessageEnvelope<T> {

    /** messageId（消息编号）表示业务侧生成的消息唯一标识。 */
    private String messageId;

    /** keys（消息键）用于 RocketMQ（消息队列）检索、排查和幂等定位。 */
    private String keys;

    /** bizType（业务类型）表示消息所属业务域。 */
    private SlowTaskBizType bizType;

    /** messageType（消息类型）表示具体慢任务类型。 */
    private SlowTaskMessageType messageType;

    /** triggerType（触发类型）表示任务由上传、手动、重试或定时等场景触发。 */
    private SlowTaskTriggerType triggerType;

    /** retryCount（重试次数）表示本消息已经被业务侧重试的次数，首次投递通常为 0。 */
    private Integer retryCount;

    /** payload（载荷）表示真正的业务消息内容。 */
    private T payload;

    /** createdAt（创建时间）表示消息信封生成时间。 */
    private LocalDateTime createdAt;
}
