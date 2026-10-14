package com.clipforge.messaging.rocketmq.rag.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KnowledgeDocumentAsyncProcessProperties（知识文档异步处理配置）。
 *
 * <p>该配置集中管理 RocketMQ（消息队列）异步文档处理的开关、主题和消费组，避免业务代码散落配置字符串。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.rag.async-process")
public class KnowledgeDocumentAsyncProcessProperties {

    /** enabled（是否启用）表示当前环境是否注册 RocketMQ（消息队列）生产者和消费者。 */
    private boolean enabled = false;

    /** topic（主题）表示知识文档处理任务投递到哪个 RocketMQ（消息队列）主题。 */
    private String topic = "clipforge-rag-document-process-topic";

    /** producerGroup（生产者组）表示发送知识文档处理消息的一组应用实例。 */
    private String producerGroup = "clipforge-rag-document-process-producer-group";

    /** consumerGroup（消费者组）表示消费知识文档处理消息的一组应用实例。 */
    private String consumerGroup = "clipforge-rag-document-process-consumer-group";
}
