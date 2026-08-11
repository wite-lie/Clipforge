package com.clipforge.infra.persistence.entity;

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
 * KnowledgeDocumentScheduleExecEntity（知识文档定时执行记录实体）。
 *
 * <p>该实体映射 cf_knowledge_document_schedule_exec（知识文档定时执行记录表），用于记录每次 URL 同步的
 * 执行过程、抓取结果和后续处理日志关联信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_knowledge_document_schedule_exec")
public class KnowledgeDocumentScheduleExecEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** execId（执行编号）是对外可查询的唯一编号。 */
    private String execId;

    /** scheduleId（定时任务编号）表示本次执行归属的定时任务。 */
    private String scheduleId;

    /** documentId（知识文档编号）表示本次执行作用的文档。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）表示本次执行所属知识库。 */
    private String knowledgeBaseId;

    /** processLogId（处理日志编号）记录本次执行最终关联的处理链路。 */
    private String processLogId;

    /** triggerType（触发类型）记录是由定时任务触发还是其他方式触发。 */
    private String triggerType;

    /** status（执行状态）记录 RUNNING、SUCCESS、FAILED、SKIPPED。 */
    private String status;

    /** sourceLocation（来源位置）记录当前执行抓取的原始 URL 地址。 */
    private String sourceLocation;

    /** previousEtag（同步前 ETag）记录变更比较前的值。 */
    private String previousEtag;

    /** previousModified（同步前 Last-Modified）记录变更比较前的值。 */
    private String previousModified;

    /** previousContentHash（同步前内容哈希）记录变更比较前的值。 */
    private String previousContentHash;

    /** currentEtag（同步后 ETag）记录本次抓取的值。 */
    private String currentEtag;

    /** currentModified（同步后 Last-Modified）记录本次抓取的值。 */
    private String currentModified;

    /** currentContentHash（同步后内容哈希）记录本次抓取的值。 */
    private String currentContentHash;

    /** contentLength（内容大小）记录本次抓取内容字节数。 */
    private Long contentLength;

    /** fetchDurationMs（抓取耗时）记录远程抓取耗时毫秒数。 */
    private Long fetchDurationMs;

    /** syncDurationMs（同步总耗时）记录从开始到完成的总耗时毫秒数。 */
    private Long syncDurationMs;

    /** errorCode（错误码）记录失败时的错误码。 */
    private String errorCode;

    /** errorMessage（错误信息）记录失败时的错误描述。 */
    private String errorMessage;

    /** startTime（开始时间）记录本次执行开始时刻。 */
    private LocalDateTime startTime;

    /** endTime（结束时间）记录本次执行结束时刻。 */
    private LocalDateTime endTime;

    /** createTime（创建时间）记录执行记录创建时刻。 */
    private LocalDateTime createTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
