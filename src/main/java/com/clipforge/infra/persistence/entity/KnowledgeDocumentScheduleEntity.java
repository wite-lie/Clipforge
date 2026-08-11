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
 * KnowledgeDocumentScheduleEntity（知识文档定时任务实体）。
 *
 * <p>该实体映射 cf_knowledge_document_schedule（知识文档定时任务表），用于保存 URL（统一资源定位符）
 * 来源文档的调度信息、租约状态和最近一次同步指纹。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_knowledge_document_schedule")
public class KnowledgeDocumentScheduleEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** scheduleId（定时任务编号）是对外使用的唯一编号。 */
    private String scheduleId;

    /** documentId（知识文档编号）表示该任务绑定的文档。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）表示该任务归属的知识库。 */
    private String knowledgeBaseId;

    /** sourceLocation（来源位置）保存原始 URL 地址。 */
    private String sourceLocation;

    /** scheduleCron（定时表达式）表示同步调度周期。 */
    private String scheduleCron;

    /** enabled（是否启用）1 表示启用，0 表示禁用。 */
    private Integer enabled;

    /** lastEtag（最近 ETag）用于变更检测。 */
    private String lastEtag;

    /** lastModified（最近修改时间）用于变更检测。 */
    private String lastModified;

    /** lastContentHash（最近内容哈希）用于变更检测。 */
    private String lastContentHash;

    /** lastSyncTime（最近同步时间）记录最近一次同步完成时间。 */
    private LocalDateTime lastSyncTime;

    /** nextSyncTime（下一次同步时间）记录下一次调度触发点。 */
    private LocalDateTime nextSyncTime;

    /** leaseOwner（租约持有者）用于防止重复调度。 */
    private String leaseOwner;

    /** leaseUntil（租约到期时间）表示当前租约的过期时间。 */
    private LocalDateTime leaseUntil;

    /** lastExecStatus（最近执行状态）记录最近一次执行结果。 */
    private String lastExecStatus;

    /** lastErrorCode（最近错误码）记录最近一次失败原因。 */
    private String lastErrorCode;

    /** lastErrorMessage（最近错误信息）记录最近一次失败描述。 */
    private String lastErrorMessage;

    /** createTime（创建时间）表示记录创建时刻。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近一次更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
