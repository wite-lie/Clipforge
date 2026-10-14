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
 * ConversationMessageEntity（会话消息实体）。
 *
 * <p>该实体映射 cf_conversation_message（会话消息表），用于保存用户输入、助手输出和系统消息摘要。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_conversation_message")
public class ConversationMessageEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** messageId（消息编号）是对外使用的消息唯一编号。 */
    private String messageId;

    /** conversationId（会话编号）表示消息所属会话。 */
    private String conversationId;

    /** runId（运行编号）用于把消息和一次 Agent（智能体）运行关联起来。 */
    private String runId;

    /** role（角色）表示 system/user/assistant/tool（系统/用户/助手/工具）。 */
    private String role;

    /** content（消息内容）保存消息正文。 */
    private String content;

    /** createTime（创建时间）保存消息创建时间。 */
    private LocalDateTime createTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
