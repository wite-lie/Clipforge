package com.clipforge.knowledge.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentScheduleEntity;
import com.clipforge.knowledge.infrastructure.persistence.mapper.KnowledgeDocumentScheduleMapper;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentScheduleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisKnowledgeDocumentScheduleRepository（MyBatis 知识文档定时任务仓储实现）。
 *
 * <p>该实现仅在 db（数据库）Profile（配置环境）下启用。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisKnowledgeDocumentScheduleRepository implements KnowledgeDocumentScheduleRepository {

    /** knowledgeDocumentScheduleMapper（知识文档定时任务映射器）负责访问任务表。 */
    private final KnowledgeDocumentScheduleMapper knowledgeDocumentScheduleMapper;

    @Override
    public KnowledgeDocumentScheduleEntity save(KnowledgeDocumentScheduleEntity entity) {
        knowledgeDocumentScheduleMapper.insert(entity);
        return entity;
    }

    @Override
    public KnowledgeDocumentScheduleEntity updateDefinition(String scheduleId, String sourceLocation, String scheduleCron, Integer enabled, LocalDateTime nextSyncTime) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity>()
                .set(KnowledgeDocumentScheduleEntity::getSourceLocation, sourceLocation)
                .set(KnowledgeDocumentScheduleEntity::getScheduleCron, scheduleCron)
                .set(KnowledgeDocumentScheduleEntity::getEnabled, enabled)
                .set(KnowledgeDocumentScheduleEntity::getNextSyncTime, nextSyncTime)
                .set(KnowledgeDocumentScheduleEntity::getUpdateTime, now)
                .eq(KnowledgeDocumentScheduleEntity::getScheduleId, scheduleId)
                .eq(KnowledgeDocumentScheduleEntity::getDeleted, 0);
        knowledgeDocumentScheduleMapper.update(null, updateWrapper);
        return findByScheduleId(scheduleId).orElseThrow();
    }

    @Override
    public Optional<KnowledgeDocumentScheduleEntity> findByScheduleId(String scheduleId) {
        LambdaQueryWrapper<KnowledgeDocumentScheduleEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentScheduleEntity>()
                .eq(KnowledgeDocumentScheduleEntity::getScheduleId, scheduleId)
                .eq(KnowledgeDocumentScheduleEntity::getDeleted, 0);
        return Optional.ofNullable(knowledgeDocumentScheduleMapper.selectOne(queryWrapper));
    }

    @Override
    public Optional<KnowledgeDocumentScheduleEntity> findByDocumentId(String documentId) {
        LambdaQueryWrapper<KnowledgeDocumentScheduleEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentScheduleEntity>()
                .eq(KnowledgeDocumentScheduleEntity::getDocumentId, documentId)
                .eq(KnowledgeDocumentScheduleEntity::getDeleted, 0);
        return Optional.ofNullable(knowledgeDocumentScheduleMapper.selectOne(queryWrapper));
    }

    @Override
    public List<KnowledgeDocumentScheduleEntity> listByKnowledgeBaseId(String knowledgeBaseId) {
        LambdaQueryWrapper<KnowledgeDocumentScheduleEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentScheduleEntity>()
                .eq(KnowledgeDocumentScheduleEntity::getKnowledgeBaseId, knowledgeBaseId)
                .eq(KnowledgeDocumentScheduleEntity::getDeleted, 0)
                .orderByDesc(KnowledgeDocumentScheduleEntity::getUpdateTime);
        return knowledgeDocumentScheduleMapper.selectList(queryWrapper);
    }

    @Override
    public List<KnowledgeDocumentScheduleEntity> listDueSchedules(LocalDateTime now, int limit) {
        LambdaQueryWrapper<KnowledgeDocumentScheduleEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentScheduleEntity>()
                .eq(KnowledgeDocumentScheduleEntity::getEnabled, 1)
                .eq(KnowledgeDocumentScheduleEntity::getDeleted, 0)
                .le(KnowledgeDocumentScheduleEntity::getNextSyncTime, now)
                .and(wrapper -> wrapper.isNull(KnowledgeDocumentScheduleEntity::getLeaseUntil)
                        .or()
                        .lt(KnowledgeDocumentScheduleEntity::getLeaseUntil, now))
                .orderByAsc(KnowledgeDocumentScheduleEntity::getNextSyncTime)
                .last("limit " + Math.max(limit, 1));
        return knowledgeDocumentScheduleMapper.selectList(queryWrapper);
    }

    @Override
    public boolean acquireLease(String scheduleId, String leaseOwner, LocalDateTime leaseUntil, LocalDateTime now) {
        LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity>()
                .set(KnowledgeDocumentScheduleEntity::getLeaseOwner, leaseOwner)
                .set(KnowledgeDocumentScheduleEntity::getLeaseUntil, leaseUntil)
                .set(KnowledgeDocumentScheduleEntity::getUpdateTime, now)
                .eq(KnowledgeDocumentScheduleEntity::getScheduleId, scheduleId)
                .eq(KnowledgeDocumentScheduleEntity::getDeleted, 0)
                .eq(KnowledgeDocumentScheduleEntity::getEnabled, 1)
                .le(KnowledgeDocumentScheduleEntity::getNextSyncTime, now)
                .and(wrapper -> wrapper.isNull(KnowledgeDocumentScheduleEntity::getLeaseUntil)
                        .or()
                        .lt(KnowledgeDocumentScheduleEntity::getLeaseUntil, now));
        return knowledgeDocumentScheduleMapper.update(null, updateWrapper) > 0;
    }

    @Override
    public KnowledgeDocumentScheduleEntity releaseLease(String scheduleId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity>()
                .set(KnowledgeDocumentScheduleEntity::getLeaseOwner, null)
                .set(KnowledgeDocumentScheduleEntity::getLeaseUntil, null)
                .set(KnowledgeDocumentScheduleEntity::getUpdateTime, now)
                .eq(KnowledgeDocumentScheduleEntity::getScheduleId, scheduleId)
                .eq(KnowledgeDocumentScheduleEntity::getDeleted, 0);
        knowledgeDocumentScheduleMapper.update(null, updateWrapper);
        return findByScheduleId(scheduleId).orElseThrow();
    }

    @Override
    public KnowledgeDocumentScheduleEntity markSuccess(String scheduleId, String lastEtag, String lastModified, String lastContentHash, LocalDateTime lastSyncTime, LocalDateTime nextSyncTime) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity>()
                .set(KnowledgeDocumentScheduleEntity::getLastEtag, lastEtag)
                .set(KnowledgeDocumentScheduleEntity::getLastModified, lastModified)
                .set(KnowledgeDocumentScheduleEntity::getLastContentHash, lastContentHash)
                .set(KnowledgeDocumentScheduleEntity::getLastSyncTime, lastSyncTime)
                .set(KnowledgeDocumentScheduleEntity::getNextSyncTime, nextSyncTime)
                .set(KnowledgeDocumentScheduleEntity::getLastExecStatus, "SUCCESS")
                .set(KnowledgeDocumentScheduleEntity::getLastErrorCode, null)
                .set(KnowledgeDocumentScheduleEntity::getLastErrorMessage, null)
                .set(KnowledgeDocumentScheduleEntity::getLeaseOwner, null)
                .set(KnowledgeDocumentScheduleEntity::getLeaseUntil, null)
                .set(KnowledgeDocumentScheduleEntity::getUpdateTime, now)
                .eq(KnowledgeDocumentScheduleEntity::getScheduleId, scheduleId)
                .eq(KnowledgeDocumentScheduleEntity::getDeleted, 0);
        knowledgeDocumentScheduleMapper.update(null, updateWrapper);
        return findByScheduleId(scheduleId).orElseThrow();
    }

    @Override
    public KnowledgeDocumentScheduleEntity markSkipped(String scheduleId, LocalDateTime lastSyncTime, LocalDateTime nextSyncTime) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity>()
                .set(KnowledgeDocumentScheduleEntity::getLastSyncTime, lastSyncTime)
                .set(KnowledgeDocumentScheduleEntity::getNextSyncTime, nextSyncTime)
                .set(KnowledgeDocumentScheduleEntity::getLastExecStatus, "SKIPPED")
                .set(KnowledgeDocumentScheduleEntity::getLastErrorCode, null)
                .set(KnowledgeDocumentScheduleEntity::getLastErrorMessage, null)
                .set(KnowledgeDocumentScheduleEntity::getLeaseOwner, null)
                .set(KnowledgeDocumentScheduleEntity::getLeaseUntil, null)
                .set(KnowledgeDocumentScheduleEntity::getUpdateTime, now)
                .eq(KnowledgeDocumentScheduleEntity::getScheduleId, scheduleId)
                .eq(KnowledgeDocumentScheduleEntity::getDeleted, 0);
        knowledgeDocumentScheduleMapper.update(null, updateWrapper);
        return findByScheduleId(scheduleId).orElseThrow();
    }

    @Override
    public KnowledgeDocumentScheduleEntity markFailed(String scheduleId, String errorCode, String errorMessage, LocalDateTime lastSyncTime, LocalDateTime nextSyncTime) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentScheduleEntity>()
                .set(KnowledgeDocumentScheduleEntity::getLastSyncTime, lastSyncTime)
                .set(KnowledgeDocumentScheduleEntity::getNextSyncTime, nextSyncTime)
                .set(KnowledgeDocumentScheduleEntity::getLastExecStatus, "FAILED")
                .set(KnowledgeDocumentScheduleEntity::getLastErrorCode, errorCode)
                .set(KnowledgeDocumentScheduleEntity::getLastErrorMessage, errorMessage)
                .set(KnowledgeDocumentScheduleEntity::getLeaseOwner, null)
                .set(KnowledgeDocumentScheduleEntity::getLeaseUntil, null)
                .set(KnowledgeDocumentScheduleEntity::getUpdateTime, now)
                .eq(KnowledgeDocumentScheduleEntity::getScheduleId, scheduleId)
                .eq(KnowledgeDocumentScheduleEntity::getDeleted, 0);
        knowledgeDocumentScheduleMapper.update(null, updateWrapper);
        return findByScheduleId(scheduleId).orElseThrow();
    }
}
