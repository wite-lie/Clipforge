package com.clipforge.contract.knowledge;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * QueryConversationMessagesResponse（查询会话消息响应）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryConversationMessagesResponse {

    private String conversationId;

    private List<ConversationMessageResponse> messages;
}
