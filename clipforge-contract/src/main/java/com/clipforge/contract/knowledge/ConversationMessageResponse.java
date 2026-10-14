package com.clipforge.contract.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConversationMessageResponse（会话消息响应）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageResponse {

    private String messageId;

    private String conversationId;

    private String runId;

    private String role;

    private String content;
}
