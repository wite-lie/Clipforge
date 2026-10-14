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
 * ConversationSummaryEntity（会话滚动摘要实体）。
 *
 * <p>该实体映射 cf_conversation_summary（会话滚动摘要表），用于记录短期记忆压缩后的摘要内容和压缩边界。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_conversation_summary")
public class ConversationSummaryEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** summaryId（摘要编号）是对外使用的唯一编号。 */
    private String summaryId;

    /** conversationId（会话编号）表示该摘要属于哪个 Conversation（会话）。 */
    private String conversationId;

    /** projectId（项目编号）表示该摘要归属的 Project（项目），可以为空。 */
    private String projectId;

    /** userId（用户编号）表示该摘要归属的用户，可以为空。 */
    private String userId;

    /** summaryContent（摘要内容）保存重写后的滚动摘要。 */
    private String summaryContent;

    /** summarizedUntilMessageId（已摘要到的消息编号）用于审计摘要覆盖边界。 */
    private String summarizedUntilMessageId;

    /** summarizedUntilTime（已摘要到的消息时间）用于判断哪些旧消息已经压缩。 */
    private LocalDateTime summarizedUntilTime;

    /** sourceMessageCount（源消息数量）表示摘要覆盖了多少条消息。 */
    private Integer sourceMessageCount;

    /** summaryVersion（摘要版本）每次重写摘要时递增。 */
    private Integer summaryVersion;

    /** tokenEstimate（词元估算）表示摘要本身估算占用的 Token（词元）数量。 */
    private Integer tokenEstimate;

    /** status（状态）表示摘要是否启用或待刷新。 */
    private String status;

    /** createTime（创建时间）表示记录创建时刻。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近一次更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
