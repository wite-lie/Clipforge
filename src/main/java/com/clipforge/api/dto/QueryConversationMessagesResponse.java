package com.clipforge.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * QueryConversationMessagesResponse（查询会话消息响应）。
 *
 * <p>该对象用于按会话编号返回多轮消息列表。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryConversationMessagesResponse {

    /** conversationId（会话编号）表示本次查询对应的会话。 */
    private String conversationId;

    /** messages（消息列表）表示会话中的多轮消息。 */
    private List<ConversationMessageResponse> messages;
}
