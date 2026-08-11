package com.clipforge.mq.rag.urlsync;

import com.clipforge.infra.persistence.enums.KnowledgeDocumentProcessTriggerType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeUrlSyncMessage（知识库 URL 同步消息）。
 *
 * <p>该消息用于 RocketMQ（消息队列）中传递 URL（统一资源定位符）知识源同步任务。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeUrlSyncMessage {

    /** messageId（消息编号）用于标识一次 URL 同步消息。 */
    private String messageId;

    /** scheduleId（定时任务编号）表示需要执行的 URL 同步任务。 */
    private String scheduleId;

    /** documentId（知识文档编号）用于消息排查和幂等关联。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）表示 URL 文档所属知识库。 */
    private String knowledgeBaseId;

    /** triggerType（触发类型）表示 SCHEDULE（定时）或 MANUAL（手动）。 */
    private KnowledgeDocumentProcessTriggerType triggerType;

    /** retryTimes（重试次数）表示该消息已经被重试的次数。 */
    private Integer retryTimes;

    /** createdAt（创建时间）表示消息创建时刻。 */
    private LocalDateTime createdAt;
}
