package com.clipforge.mq.common;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * AbstractRocketMqSlowTaskProducer（RocketMQ 慢任务生产者模板）用于统一消息发送、日志和异常处理。
 *
 * @param <T> 业务消息类型
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractRocketMqSlowTaskProducer<T> {

    /** rocketMQTemplate（RocketMQ 消息模板）用于执行真实消息发送。 */
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * buildSendCommand（构造发送命令）由具体业务生产者声明 Topic（主题）、Tag（标签）、Keys（消息键）等差异点。
     *
     * @param payload 业务消息
     * @return 慢任务发送命令
     */
    protected abstract SlowTaskSendCommand buildSendCommand(T payload);

    /**
     * sendMessage（发送消息）定义 RocketMQ（消息队列）慢任务发送的固定流程。
     *
     * @param payload 业务消息
     * @return RocketMQ（消息队列）发送结果
     */
    public SendResult sendMessage(T payload) {
        // 构造本次发送命令。
        SlowTaskSendCommand command = buildSendCommand(payload);
        // 校验消息载荷和发送命令，避免空消息进入 RocketMQ（消息队列）。
        validate(payload, command);
        // 构造统一消息信封。
        SlowTaskMessageEnvelope<T> envelope = buildEnvelope(payload, command);
        // 构造 Spring Message（Spring 消息对象），设置 RocketMQ Keys（消息键）和 Tags（标签）。
        Message<SlowTaskMessageEnvelope<T>> message = buildSpringMessage(envelope, command);
        // 拼接目标地址，RocketMQ Spring 使用 topic:tag 表达目标标签。
        String destination = resolveDestination(command);
        try {
            // 根据 delayTimeMillis（延时投递毫秒时间点）决定发送普通消息还是延时消息。
            SendResult sendResult = command.getDelayTimeMillis() == null
                    ? rocketMQTemplate.syncSend(destination, message, timeoutMillis(command))
                    : rocketMQTemplate.syncSendDeliverTimeMills(destination, message, command.getDelayTimeMillis());
            // 返回发送结果。
            return sendResult;
        } catch (RuntimeException exception) {
            // 打印统一失败日志。
            printFailure(command, envelope, exception);
            // 包装为业务异常，让应用层能识别异步任务投递失败。
            throw new BusinessException(ErrorCode.ASYNC_PROCESS_UNAVAILABLE,
                    "RocketMQ（消息队列）慢任务消息发送失败，eventName=" + command.getEventName() + "，原因：" + exception.getMessage(),
                    exception);
        }
    }

    /**
     * buildEnvelope（构造消息信封）用于把业务消息包装成统一慢任务消息。
     *
     * @param payload 业务消息
     * @param command 发送命令
     * @return 慢任务消息信封
     */
    private SlowTaskMessageEnvelope<T> buildEnvelope(T payload, SlowTaskSendCommand command) {
        // 使用业务 messageId；如果业务侧未传，则生成兜底编号。
        String messageId = textOrDefault(command.getMessageId(), UUID.randomUUID().toString());
        // 使用业务 keys；如果业务侧未传，则用 messageId 作为排查键。
        String keys = textOrDefault(command.getKeys(), messageId);
        // 构造统一消息信封。
        return SlowTaskMessageEnvelope.<T>builder()
                .messageId(messageId)
                .keys(keys)
                .bizType(command.getBizType())
                .messageType(command.getMessageType())
                .triggerType(command.getTriggerType())
                .retryCount(command.getRetryCount() == null ? 0 : command.getRetryCount())
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * buildSpringMessage（构造 Spring 消息）用于设置 RocketMQ（消息队列）发送头。
     *
     * @param envelope 慢任务消息信封
     * @param command 发送命令
     * @return Spring 消息对象
     */
    private Message<SlowTaskMessageEnvelope<T>> buildSpringMessage(SlowTaskMessageEnvelope<T> envelope, SlowTaskSendCommand command) {
        // 创建消息构建器。
        MessageBuilder<SlowTaskMessageEnvelope<T>> builder = MessageBuilder
                .withPayload(envelope)
                .setHeader(MessageConst.PROPERTY_KEYS, envelope.getKeys());
        // 如果设置了 tag（标签），同步写入消息头。
        if (command.getTag() != null && !command.getTag().isBlank()) {
            builder.setHeader(MessageConst.PROPERTY_TAGS, command.getTag());
        }
        // 构建 Spring 消息对象。
        return builder.build();
    }

    /**
     * resolveDestination（解析目标地址）用于拼接 topic:tag 格式的 RocketMQ（消息队列）目标。
     *
     * @param command 发送命令
     * @return RocketMQ 目标地址
     */
    private String resolveDestination(SlowTaskSendCommand command) {
        // 未配置 tag（标签）时直接返回 topic（主题）。
        if (command.getTag() == null || command.getTag().isBlank()) {
            return command.getTopic();
        }
        // 配置 tag（标签）时拼接 topic:tag。
        return command.getTopic() + ":" + command.getTag();
    }

    /**
     * validate（校验）用于在发送前阻断明显不合法的消息。
     *
     * @param payload 业务消息
     * @param command 发送命令
     */
    private void validate(T payload, SlowTaskSendCommand command) {
        // 业务消息不能为空。
        Objects.requireNonNull(payload, "RocketMQ 慢任务业务消息不能为空");
        // 发送命令不能为空。
        Objects.requireNonNull(command, "RocketMQ 慢任务发送命令不能为空");
        // topic（主题）不能为空。
        if (command.getTopic() == null || command.getTopic().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "RocketMQ 慢任务 topic（主题）不能为空");
        }
        // messageType（消息类型）不能为空，方便消费端和日志识别。
        if (command.getMessageType() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "RocketMQ 慢任务 messageType（消息类型）不能为空");
        }
    }

    /**
     * timeoutMillis（超时毫秒数）用于给未配置发送超时的任务提供默认值。
     *
     * @param command 发送命令
     * @return 发送超时毫秒数
     */
    private long timeoutMillis(SlowTaskSendCommand command) {
        // 未配置时默认 3000 毫秒。
        return command.getSentTimeoutMillis() == null ? 3000L : Math.max(1L, command.getSentTimeoutMillis());
    }

    /**
     * textOrDefault（文本兜底）用于处理空字符串。
     *
     * @param value 原始文本
     * @param defaultValue 默认文本
     * @return 非空文本
     */
    private String textOrDefault(String value, String defaultValue) {
        // 原始文本为空时返回默认值。
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        // 原始文本不为空时直接返回。
        return value;
    }

    /**
     * printFailure（打印失败日志）用于统一 RocketMQ（消息队列）发送失败观测信息。
     *
     * @param command 发送命令
     * @param envelope 消息信封
     * @param exception 发送异常
     */
    private void printFailure(SlowTaskSendCommand command, SlowTaskMessageEnvelope<T> envelope, RuntimeException exception) {
        log.error("RocketMQ 慢任务消息发送失败，eventName={}，messageType={}，messageId={}，异常类型={}，原因={}",
                command.getEventName(), command.getMessageType(), envelope.getMessageId(),
                exception.getClass().getName(), exception.getMessage(), exception);
    }
}
