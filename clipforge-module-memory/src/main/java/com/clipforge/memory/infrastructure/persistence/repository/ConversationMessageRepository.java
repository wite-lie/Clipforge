package com.clipforge.memory.infrastructure.persistence.repository;

import com.clipforge.memory.infrastructure.persistence.entity.ConversationMessageEntity;
import java.util.List;

/**
 * ConversationMessageRepository（会话消息仓储）。
 *
 * <p>该接口负责保存和查询 Conversation Message（会话消息）。
 */
public interface ConversationMessageRepository {

    /**
     * append（追加）用于向会话中追加一条消息。
     *
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @param role 消息角色
     * @param content 消息内容
     * @return 已保存消息实体
     */
    ConversationMessageEntity append(String conversationId, String runId, String role, String content);

    /**
     * listByConversationId（按会话编号查询列表）用于读取会话消息。
     *
     * @param conversationId 会话编号
     * @return 会话消息实体列表
     */
    List<ConversationMessageEntity> listByConversationId(String conversationId);
}
