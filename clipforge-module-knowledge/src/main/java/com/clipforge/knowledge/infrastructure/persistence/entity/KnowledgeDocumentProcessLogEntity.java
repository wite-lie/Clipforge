package com.clipforge.knowledge.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeDocumentProcessLogEntity（知识文档处理日志实体）。
 *
 * <p>该实体映射 cf_knowledge_document_process_log（知识文档处理日志表），用于记录一次知识文档从解析、切片到 Embedding（嵌入）向量入库的处理链路。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_knowledge_document_process_log")
public class KnowledgeDocumentProcessLogEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** processLogId（处理日志编号）是对外查询和接口返回使用的唯一编号。 */
    private String processLogId;

    /** documentId（知识文档编号）表示本次处理的目标文档。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）表示文档所属知识库。 */
    private String knowledgeBaseId;

    /** triggerType（触发类型）表示本次处理由手动、定时任务或消息队列触发。 */
    private String triggerType;

    /** status（处理状态）表示 RUNNING（运行中）、SUCCESS（成功）、FAILED（失败）或 SKIPPED（跳过）。 */
    private String status;

    /** phase（处理阶段）表示当前或最终阶段，例如 PARSE_CHUNK（解析切片）或 EMBEDDING（嵌入）。 */
    private String phase;

    /** parseDurationMs（解析耗时毫秒）当前同步实现中和切片耗时合并统计时可以为空。 */
    private Long parseDurationMs;

    /** chunkDurationMs（切片耗时毫秒）表示解析加切片阶段的整体耗时。 */
    private Long chunkDurationMs;

    /** embeddingDurationMs（嵌入耗时毫秒）表示 Embedding（嵌入）模型调用和向量持久化阶段的整体耗时。 */
    private Long embeddingDurationMs;

    /** persistDurationMs（持久化耗时毫秒）当前第一版由各子服务内部短事务完成，日志中暂不单独拆分。 */
    private Long persistDurationMs;

    /** totalDurationMs（总耗时毫秒）表示完整处理链路耗时。 */
    private Long totalDurationMs;

    /** chunkCount（切片数量）表示本次处理产生或参与向量化的有效切片数量。 */
    private Integer chunkCount;

    /** errorCode（错误码）表示失败时的业务错误码。 */
    private String errorCode;

    /** errorMessage（错误消息）表示失败时的可读原因。 */
    private String errorMessage;

    /** startTime（开始时间）表示本次处理开始时刻。 */
    private LocalDateTime startTime;

    /** endTime（结束时间）表示本次处理结束时刻。 */
    private LocalDateTime endTime;

    /** createTime（创建时间）表示日志记录创建时刻。 */
    private LocalDateTime createTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
