package com.clipforge.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeDocumentProcessStatusResponse（知识文档处理状态响应）。
 *
 * <p>该对象用于接口返回知识文档异步处理状态，既可以来自 Redis（缓存），也可以来自 PostgreSQL（关系数据库）处理日志。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentProcessStatusResponse {

    /** documentId（知识文档编号）表示当前状态所属文档。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）表示文档所属知识库。 */
    private String knowledgeBaseId;

    /** processLogId（处理日志编号）表示当前状态关联的处理日志。 */
    private String processLogId;

    /** status（处理状态）表示 RUNNING（运行中）、SUCCESS（成功）、FAILED（失败）或 SKIPPED（跳过）。 */
    private String status;

    /** phase（处理阶段）表示 PARSE_CHUNK（解析切片）、EMBEDDING（嵌入）或 READY（可检索）。 */
    private String phase;

    /** triggerType（触发类型）表示 MANUAL（手动）、SCHEDULE（定时）、MQ_RETRY（消息重试）或 SYSTEM_SEED（系统种子）。 */
    private String triggerType;

    /** progressPercent（进度百分比）用于展示粗粒度处理进度。 */
    private Integer progressPercent;

    /** message（状态消息）用于展示当前状态的人类可读说明。 */
    private String message;

    /** errorCode（错误码）用于失败时展示机器可读错误编码。 */
    private String errorCode;

    /** errorMessage（错误消息）用于失败时展示人类可读失败原因。 */
    private String errorMessage;

    /** chunkCount（切片数量）表示本次处理生成或参与向量化的切片数量。 */
    private Integer chunkCount;

    /** chunkDurationMs（解析切片耗时毫秒）表示解析和切片阶段耗时。 */
    private Long chunkDurationMs;

    /** embeddingDurationMs（嵌入耗时毫秒）表示嵌入和向量入库阶段耗时。 */
    private Long embeddingDurationMs;

    /** totalDurationMs（总耗时毫秒）表示完整处理链路耗时。 */
    private Long totalDurationMs;

    /** startedAt（开始时间）表示处理开始时刻。 */
    private LocalDateTime startedAt;

    /** updatedAt（更新时间）表示状态最近刷新时刻。 */
    private LocalDateTime updatedAt;

    /** endedAt（结束时间）表示处理结束时刻，运行中可以为空。 */
    private LocalDateTime endedAt;

    /** source（状态来源）表示 REDIS（缓存）、DATABASE（数据库）或 DOCUMENT（文档状态兜底）。 */
    private String source;

    /** cacheHit（是否命中缓存）表示本次查询是否直接命中 Redis（缓存）。 */
    private Boolean cacheHit;

    /** recoverable（是否可恢复）表示当前文档处理状态是否允许人工恢复。 */
    private Boolean recoverable;

    /** retryable（是否可重试）表示当前文档处理状态是否允许重新投递处理任务。 */
    private Boolean retryable;

    /** runningStuck（运行中是否卡住）表示运行中任务是否已经超过恢复超时阈值。 */
    private Boolean runningStuck;

    /** retryCount（已重试次数）表示当前文档已经通过 MQ_RETRY（消息重试）触发过几次。 */
    private Integer retryCount;

    /** maxRetryCount（最大重试次数）表示当前配置允许的最大重试次数。 */
    private Integer maxRetryCount;

    /** suggestedRetryStage（建议重试阶段）表示后端根据最近失败阶段推断出的恢复入口。 */
    private String suggestedRetryStage;

    /** recoverySuggestion（恢复建议）用于向前端展示下一步处理建议。 */
    private String recoverySuggestion;
}
