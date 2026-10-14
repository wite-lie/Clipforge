package com.clipforge.contract.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConversationResponse（会话响应）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private String conversationId;

    private String projectId;

    private String userId;

    private String title;

    private String status;
}
