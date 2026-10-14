package com.clipforge.memory.infrastructure.persistence.repository;

import com.clipforge.memory.infrastructure.persistence.entity.ConversationEntity;
import java.util.List;
import java.util.Optional;

/**
 * ConversationRepository（会话仓储）。
 *
 * <p>该接口负责保存和查询 Conversation（会话）。
 */
public interface ConversationRepository {

    /**
     * create（创建）用于在项目下创建一次会话。
     *
     * @param projectId 项目编号
     * @param userId 用户编号
     * @param title 会话标题
     * @return 已保存会话实体
     */
    ConversationEntity create(String projectId, String userId, String title);

    /**
     * findByConversationId（按会话编号查询）用于读取会话详情。
     *
     * @param conversationId 会话编号
     * @return 可能存在的会话实体
     */
    Optional<ConversationEntity> findByConversationId(String conversationId);

    /**
     * listByProjectId（按项目编号查询列表）用于读取一个项目下的会话列表。
     *
     * @param projectId 项目编号
     * @return 会话实体列表
     */
    List<ConversationEntity> listByProjectId(String projectId);

    /**
     * updateTitle（更新标题）用于修改用户可见的会话名称。
     *
     * @param conversationId 会话编号
     * @param title 新会话标题
     */
    void updateTitle(String conversationId, String title);

    /**
     * logicalDeleteByConversationId（按会话编号逻辑删除）用于前端删除会话时隐藏该会话。
     *
     * @param conversationId 会话编号
     */
    void logicalDeleteByConversationId(String conversationId);
}
