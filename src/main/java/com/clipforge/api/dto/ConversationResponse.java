package com.clipforge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConversationResponse（会话响应）。
 *
 * <p>该对象用于接口返回 Conversation（会话）基础信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    /** conversationId（会话编号）表示对外使用的会话唯一编号。 */
    private String conversationId;

    /** projectId（项目编号）表示会话所属项目。 */
    private String projectId;

    /** userId（用户编号）表示会话所属用户。 */
    private String userId;

    /** title（会话标题）表示本次讨论主题。 */
    private String title;

    /** status（会话状态）表示 ACTIVE（进行中）或 CLOSED（已关闭）。 */
    private String status;
}
