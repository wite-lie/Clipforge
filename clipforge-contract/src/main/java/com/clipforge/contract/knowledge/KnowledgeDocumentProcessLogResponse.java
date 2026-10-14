package com.clipforge.contract.knowledge;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeDocumentProcessLogResponse（知识文档处理日志响应）。
 *
 * <p>该对象用于管理接口展示某个文档的完整处理历史。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentProcessLogResponse {

    /** processLogId（处理日志编号）。 */
    private String processLogId;

    /** documentId（知识文档编号）。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）。 */
    private String knowledgeBaseId;

    /** triggerType（触发类型）。 */
    private String triggerType;

    /** status（处理状态）。 */
    private String status;

    /** phase（处理阶段）。 */
    private String phase;

    /** parseDurationMs（解析耗时毫秒）。 */
    private Long parseDurationMs;

    /** chunkDurationMs（切片耗时毫秒）。 */
    private Long chunkDurationMs;

    /** embeddingDurationMs（向量化耗时毫秒）。 */
    private Long embeddingDurationMs;

    /** persistDurationMs（持久化耗时毫秒）。 */
    private Long persistDurationMs;

    /** totalDurationMs（总耗时毫秒）。 */
    private Long totalDurationMs;

    /** chunkCount（切片数量）。 */
    private Integer chunkCount;

    /** errorCode（错误码）。 */
    private String errorCode;

    /** errorMessage（错误消息）。 */
    private String errorMessage;

    /** startTime（开始时间）。 */
    private LocalDateTime startTime;

    /** endTime（结束时间）。 */
    private LocalDateTime endTime;

    /** createTime（创建时间）。 */
    private LocalDateTime createTime;
}
