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
 * ConversationEntity（会话实体）。
 *
 * <p>该实体映射 cf_conversation（会话表），用于保存一个项目下的一次连续创作讨论。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_conversation")
public class ConversationEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** conversationId（会话编号）是对外使用的会话唯一编号。 */
    private String conversationId;

    /** projectId（项目编号）表示当前会话所属项目。 */
    private String projectId;

    /** userId（用户编号）表示会话所属用户，当前学习阶段可以为空。 */
    private String userId;

    /** title（会话标题）表示本次讨论的主题。 */
    private String title;

    /** status（会话状态）表示 ACTIVE（进行中）或 CLOSED（已关闭）。 */
    private String status;

    /** createTime（创建时间）保存会话创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）保存会话最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
