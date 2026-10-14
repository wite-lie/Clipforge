package com.clipforge.messaging.rocketmq.rag.document;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeDocumentProcessMessage（知识文档处理消息）。
 *
 * <p>该对象用于 RocketMQ（消息队列）中传递知识文档异步处理任务，生产者写入该消息，消费者根据其中的 documentId（文档编号）和 processLogId（处理日志编号）执行处理。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentProcessMessage {

    /** messageId（消息编号）用于标识一次异步处理消息，后续可用于幂等和排查重复投递。 */
    private String messageId;

    /** documentId（知识文档编号）表示需要处理的知识文档。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）表示文档所属知识库。 */
    private String knowledgeBaseId;

    /** processLogId（处理日志编号）用于关联 cf_knowledge_document_process_log（知识文档处理日志表）。 */
    private String processLogId;

    /** triggerType（触发类型）表示 MANUAL（手动）、SCHEDULE（定时）、MQ_RETRY（消息重试）或 SYSTEM_SEED（系统种子）。 */
    private String triggerType;

    /** retryTimes（重试次数）表示该消息已经被重试的次数，首次投递为 0。 */
    private Integer retryTimes;

    /** retryStage（重试阶段）表示恢复任务希望从哪个阶段重新处理，普通首次处理可以为空。 */
    private KnowledgeDocumentRetryStage retryStage;

    /** createdAt（创建时间）表示消息被创建的时间。 */
    private LocalDateTime createdAt;

    /**
     * triggerTypeValue（触发类型值）用于获取可写入数据库或消息体的字符串值。
     *
     * @return 触发类型字符串
     */
    public String triggerTypeValue() {
        if (triggerType == null || triggerType.isBlank()) {
            return "MANUAL";
        }
        return triggerType;
    }
}
