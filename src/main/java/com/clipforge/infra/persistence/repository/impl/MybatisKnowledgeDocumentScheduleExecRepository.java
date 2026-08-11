package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentScheduleExecEntity;
import com.clipforge.infra.persistence.mapper.KnowledgeDocumentScheduleExecMapper;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentScheduleExecRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisKnowledgeDocumentScheduleExecRepository（MyBatis 知识文档定时执行记录仓储实现）。
 *
 * <p>该实现仅在 db（数据库）Profile（配置环境）下启用。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisKnowledgeDocumentScheduleExecRepository implements KnowledgeDocumentScheduleExecRepository {

    /** knowledgeDocumentScheduleExecMapper（知识文档定时执行记录映射器）负责访问执行记录表。 */
    private final KnowledgeDocumentScheduleExecMapper knowledgeDocumentScheduleExecMapper;

    @Override
    public KnowledgeDocumentScheduleExecEntity save(KnowledgeDocumentScheduleExecEntity entity) {
        knowledgeDocumentScheduleExecMapper.insert(entity);
        return entity;
    }

    @Override
    public Optional<KnowledgeDocumentScheduleExecEntity> findByExecId(String execId) {
        LambdaQueryWrapper<KnowledgeDocumentScheduleExecEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentScheduleExecEntity>()
                .eq(KnowledgeDocumentScheduleExecEntity::getExecId, execId)
                .eq(KnowledgeDocumentScheduleExecEntity::getDeleted, 0);
        return Optional.ofNullable(knowledgeDocumentScheduleExecMapper.selectOne(queryWrapper));
    }

    @Override
    public List<KnowledgeDocumentScheduleExecEntity> listByScheduleId(String scheduleId, int limit) {
        LambdaQueryWrapper<KnowledgeDocumentScheduleExecEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentScheduleExecEntity>()
                .eq(KnowledgeDocumentScheduleExecEntity::getScheduleId, scheduleId)
                .eq(KnowledgeDocumentScheduleExecEntity::getDeleted, 0)
                .orderByDesc(KnowledgeDocumentScheduleExecEntity::getStartTime)
                .last("limit " + Math.max(limit, 1));
        return knowledgeDocumentScheduleExecMapper.selectList(queryWrapper);
    }

    @Override
    public KnowledgeDocumentScheduleExecEntity markSuccess(String execId, String processLogId, String currentEtag, String currentModified, String currentContentHash, Long contentLength, Long fetchDurationMs, Long syncDurationMs) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentScheduleExecEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentScheduleExecEntity>()
                .set(KnowledgeDocumentScheduleExecEntity::getProcessLogId, processLogId)
                .set(KnowledgeDocumentScheduleExecEntity::getStatus, "SUCCESS")
                .set(KnowledgeDocumentScheduleExecEntity::getCurrentEtag, currentEtag)
                .set(KnowledgeDocumentScheduleExecEntity::getCurrentModified, currentModified)
                .set(KnowledgeDocumentScheduleExecEntity::getCurrentContentHash, currentContentHash)
                .set(KnowledgeDocumentScheduleExecEntity::getContentLength, contentLength)
                .set(KnowledgeDocumentScheduleExecEntity::getFetchDurationMs, fetchDurationMs)
                .set(KnowledgeDocumentScheduleExecEntity::getSyncDurationMs, syncDurationMs)
                .set(KnowledgeDocumentScheduleExecEntity::getErrorCode, null)
                .set(KnowledgeDocumentScheduleExecEntity::getErrorMessage, null)
                .set(KnowledgeDocumentScheduleExecEntity::getEndTime, now)
                .eq(KnowledgeDocumentScheduleExecEntity::getExecId, execId)
                .eq(KnowledgeDocumentScheduleExecEntity::getDeleted, 0);
        knowledgeDocumentScheduleExecMapper.update(null, updateWrapper);
        return findByExecId(execId).orElseThrow();
    }

    @Override
    public KnowledgeDocumentScheduleExecEntity markSkipped(String execId, String currentEtag, String currentModified, String currentContentHash, Long contentLength, Long fetchDurationMs, Long syncDurationMs, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentScheduleExecEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentScheduleExecEntity>()
                .set(KnowledgeDocumentScheduleExecEntity::getStatus, "SKIPPED")
                .set(KnowledgeDocumentScheduleExecEntity::getCurrentEtag, currentEtag)
                .set(KnowledgeDocumentScheduleExecEntity::getCurrentModified, currentModified)
                .set(KnowledgeDocumentScheduleExecEntity::getCurrentContentHash, currentContentHash)
                .set(KnowledgeDocumentScheduleExecEntity::getContentLength, contentLength)
                .set(KnowledgeDocumentScheduleExecEntity::getFetchDurationMs, fetchDurationMs)
                .set(KnowledgeDocumentScheduleExecEntity::getSyncDurationMs, syncDurationMs)
                .set(KnowledgeDocumentScheduleExecEntity::getErrorMessage, errorMessage)
                .set(KnowledgeDocumentScheduleExecEntity::getEndTime, now)
                .eq(KnowledgeDocumentScheduleExecEntity::getExecId, execId)
                .eq(KnowledgeDocumentScheduleExecEntity::getDeleted, 0);
        knowledgeDocumentScheduleExecMapper.update(null, updateWrapper);
        return findByExecId(execId).orElseThrow();
    }

    @Override
    public KnowledgeDocumentScheduleExecEntity markFailed(String execId, String errorCode, String errorMessage, Long fetchDurationMs, Long syncDurationMs) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentScheduleExecEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentScheduleExecEntity>()
                .set(KnowledgeDocumentScheduleExecEntity::getStatus, "FAILED")
                .set(KnowledgeDocumentScheduleExecEntity::getErrorCode, errorCode)
                .set(KnowledgeDocumentScheduleExecEntity::getErrorMessage, errorMessage)
                .set(KnowledgeDocumentScheduleExecEntity::getFetchDurationMs, fetchDurationMs)
                .set(KnowledgeDocumentScheduleExecEntity::getSyncDurationMs, syncDurationMs)
                .set(KnowledgeDocumentScheduleExecEntity::getEndTime, now)
                .eq(KnowledgeDocumentScheduleExecEntity::getExecId, execId)
                .eq(KnowledgeDocumentScheduleExecEntity::getDeleted, 0);
        knowledgeDocumentScheduleExecMapper.update(null, updateWrapper);
        return findByExecId(execId).orElseThrow();
    }
}
