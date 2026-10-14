package com.clipforge.contract.knowledge;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeDocumentRecoveryResponse（知识文档恢复响应）。
 *
 * <p>该对象用于返回某个 RAG（检索增强生成）知识文档当前是否可恢复、建议如何恢复以及最近一次处理日志信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentRecoveryResponse {

    /** documentId（知识文档编号）表示当前恢复建议所属文档。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）表示文档所属知识库。 */
    private String knowledgeBaseId;

    /** latestProcessLogId（最近处理日志编号）表示最近一次处理记录。 */
    private String latestProcessLogId;

    /** latestStatus（最近状态）表示 RUNNING（运行中）、SUCCESS（成功）或 FAILED（失败）。 */
    private String latestStatus;

    /** latestPhase（最近阶段）表示最近一次处理停留或失败的阶段。 */
    private String latestPhase;

    /** latestErrorCode（最近错误码）表示最近失败的机器可读错误编码。 */
    private String latestErrorCode;

    /** latestErrorMessage（最近错误消息）表示最近失败的人类可读原因。 */
    private String latestErrorMessage;

    /** recoverable（是否可恢复）表示当前状态是否允许人工重试。 */
    private Boolean recoverable;

    /** retryable（是否可重试）表示当前状态是否允许重新投递处理消息。 */
    private Boolean retryable;

    /** runningStuck（运行中是否卡住）表示 RUNNING（运行中）任务是否已经超过超时阈值。 */
    private Boolean runningStuck;

    /** retryCount（已重试次数）表示当前文档已经通过 MQ_RETRY（消息重试）触发过几次。 */
    private Integer retryCount;

    /** maxRetryCount（最大重试次数）表示当前配置允许的最大重试次数。 */
    private Integer maxRetryCount;

    /** suggestedRetryStage（建议重试阶段）表示后端根据失败阶段推断出的恢复入口。 */
    private String suggestedRetryStage;

    /** recoverySuggestion（恢复建议）是给前端或管理员展示的可读说明。 */
    private String recoverySuggestion;

    /** latestStartTime（最近开始时间）表示最近一次处理开始时刻。 */
    private LocalDateTime latestStartTime;

    /** latestEndTime（最近结束时间）表示最近一次处理结束时刻。 */
    private LocalDateTime latestEndTime;
}
