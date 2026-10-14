package com.clipforge.memory.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.memory.infrastructure.persistence.entity.ConversationSummaryEntity;
import com.clipforge.memory.infrastructure.persistence.enums.ConversationSummaryStatus;
import com.clipforge.memory.infrastructure.persistence.mapper.ConversationSummaryMapper;
import com.clipforge.memory.infrastructure.persistence.repository.ConversationSummaryRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisConversationSummaryRepository（MyBatis 会话滚动摘要仓储实现）。
 *
 * <p>该实现仅在 db（数据库）Profile（配置环境）下启用。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisConversationSummaryRepository implements ConversationSummaryRepository {

    /** conversationSummaryMapper（会话滚动摘要映射器）负责访问会话滚动摘要表。 */
    private final ConversationSummaryMapper conversationSummaryMapper;

    @Override
    public ConversationSummaryEntity save(ConversationSummaryEntity entity) {
        conversationSummaryMapper.insert(entity);
        return entity;
    }

    @Override
    public Optional<ConversationSummaryEntity> findActiveByConversationId(String conversationId) {
        LambdaQueryWrapper<ConversationSummaryEntity> queryWrapper = new LambdaQueryWrapper<ConversationSummaryEntity>()
                .eq(ConversationSummaryEntity::getConversationId, conversationId)
                .eq(ConversationSummaryEntity::getStatus, ConversationSummaryStatus.ACTIVE.value())
                .eq(ConversationSummaryEntity::getDeleted, 0);
        return Optional.ofNullable(conversationSummaryMapper.selectOne(queryWrapper));
    }

    @Override
    public ConversationSummaryEntity upsertSummary(ConversationSummaryEntity entity) {
        Optional<ConversationSummaryEntity> existing = findAnyByConversationId(entity.getConversationId());
        if (existing.isEmpty()) {
            conversationSummaryMapper.insert(entity);
            return entity;
        }
        ConversationSummaryEntity old = existing.get();
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<ConversationSummaryEntity> updateWrapper = new LambdaUpdateWrapper<ConversationSummaryEntity>()
                .set(ConversationSummaryEntity::getProjectId, entity.getProjectId())
                .set(ConversationSummaryEntity::getUserId, entity.getUserId())
                .set(ConversationSummaryEntity::getSummaryContent, entity.getSummaryContent())
                .set(ConversationSummaryEntity::getSummarizedUntilMessageId, entity.getSummarizedUntilMessageId())
                .set(ConversationSummaryEntity::getSummarizedUntilTime, entity.getSummarizedUntilTime())
                .set(ConversationSummaryEntity::getSourceMessageCount, entity.getSourceMessageCount())
                .set(ConversationSummaryEntity::getSummaryVersion, (old.getSummaryVersion() == null ? 1 : old.getSummaryVersion()) + 1)
                .set(ConversationSummaryEntity::getTokenEstimate, entity.getTokenEstimate())
                .set(ConversationSummaryEntity::getStatus, ConversationSummaryStatus.ACTIVE.value())
                .set(ConversationSummaryEntity::getUpdateTime, now)
                .eq(ConversationSummaryEntity::getConversationId, entity.getConversationId())
                .eq(ConversationSummaryEntity::getDeleted, 0);
        conversationSummaryMapper.update(null, updateWrapper);
        return findActiveByConversationId(entity.getConversationId()).orElse(entity);
    }

    @Override
    public boolean markStale(String conversationId, LocalDateTime now) {
        LambdaUpdateWrapper<ConversationSummaryEntity> updateWrapper = new LambdaUpdateWrapper<ConversationSummaryEntity>()
                .set(ConversationSummaryEntity::getStatus, ConversationSummaryStatus.STALE.value())
                .set(ConversationSummaryEntity::getUpdateTime, now)
                .eq(ConversationSummaryEntity::getConversationId, conversationId)
                .eq(ConversationSummaryEntity::getDeleted, 0);
        return conversationSummaryMapper.update(null, updateWrapper) > 0;
    }

    /**
     * findAnyByConversationId（按会话查询任意摘要）用于 upsert（插入或更新）时判断是否已有摘要。
     *
     * @param conversationId 会话编号
     * @return 可能存在的会话滚动摘要
     */
    private Optional<ConversationSummaryEntity> findAnyByConversationId(String conversationId) {
        LambdaQueryWrapper<ConversationSummaryEntity> queryWrapper = new LambdaQueryWrapper<ConversationSummaryEntity>()
                .eq(ConversationSummaryEntity::getConversationId, conversationId)
                .eq(ConversationSummaryEntity::getDeleted, 0);
        return Optional.ofNullable(conversationSummaryMapper.selectOne(queryWrapper));
    }
}
