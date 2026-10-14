package com.clipforge.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ConversationResolution（会话归属解析结果）。
 *
 * <p>该对象表示一次创建方案请求最终归属到哪个 Project（项目）和 Conversation（会话）。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResolution {

    /** projectId（项目编号）表示本次运行最终所属项目。 */
    private String projectId;

    /** conversationId（会话编号）表示本次运行最终所属会话。 */
    private String conversationId;

    /** autoCreatedProject（是否自动创建项目）表示是否为本次请求新建了默认 Project（项目）。 */
    private boolean autoCreatedProject;

    /** autoCreatedConversation（是否自动创建会话）表示是否为本次请求新建了 Conversation（会话）。 */
    private boolean autoCreatedConversation;
}
