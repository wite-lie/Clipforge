package com.clipforge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConversationMessageResponse（会话消息响应）。
 *
 * <p>该对象用于接口返回一条 Conversation Message（会话消息）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageResponse {

    /** messageId（消息编号）表示对外使用的消息唯一编号。 */
    private String messageId;

    /** conversationId（会话编号）表示消息所属会话。 */
    private String conversationId;

    /** runId（运行编号）表示消息关联的 Agent（智能体）运行。 */
    private String runId;

    /** role（角色）表示 system/user/assistant/tool（系统/用户/助手/工具）。 */
    private String role;

    /** content（消息内容）保存消息正文或摘要。 */
    private String content;
}
