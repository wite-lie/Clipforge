package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.ConversationSummaryEntity;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ConversationSummaryRepository（会话滚动摘要仓储）。
 *
 * <p>该接口负责保存和更新 Short-term Memory（短期记忆）的滚动摘要状态。
 */
public interface ConversationSummaryRepository {

    /**
     * save（保存）用于创建会话滚动摘要。
     *
     * @param entity 会话滚动摘要实体
     * @return 已保存实体
     */
    ConversationSummaryEntity save(ConversationSummaryEntity entity);

    /**
     * findActiveByConversationId（按会话查询启用摘要）用于构建短期记忆上下文。
     *
     * @param conversationId 会话编号
     * @return 可能存在的会话滚动摘要
     */
    Optional<ConversationSummaryEntity> findActiveByConversationId(String conversationId);

    /**
     * upsertSummary（创建或更新摘要）用于重写滚动摘要并推进压缩边界。
     *
     * @param entity 新摘要实体
     * @return 已保存或更新后的摘要实体
     */
    ConversationSummaryEntity upsertSummary(ConversationSummaryEntity entity);

    /**
     * markStale（标记待刷新）用于在上下文接近上限时标记摘要需要重写。
     *
     * @param conversationId 会话编号
     * @param now 当前时间
     * @return 是否更新成功
     */
    boolean markStale(String conversationId, LocalDateTime now);
}
