package com.clipforge.memory.infrastructure.persistence.entity;

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
 * AgentTraceEventEntity（智能体执行链路事件实体）。
 *
 * <p>该实体映射数据库表 cf_agent_trace_event（Agent 执行链路事件表），用于保存一次运行过程中的关键事件。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_agent_trace_event")
public class AgentTraceEventEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** eventId（事件编号）是单条执行链路事件的唯一编号。 */
    private String eventId;

    /** runId（运行编号）表示事件所属的 Agent（智能体）运行。 */
    private String runId;

    /** eventType（事件类型）表示事件的机器可读标识。 */
    private String eventType;

    /** message（事件消息）表示事件的人类可读说明。 */
    private String message;

    /** detail（事件详情）表示事件补充信息。 */
    private String detail;

    /** occurredAt（发生时间）表示事件发生的时间。 */
    private LocalDateTime occurredAt;

    /** createTime（创建时间）保存事件入库时间。 */
    private LocalDateTime createTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
