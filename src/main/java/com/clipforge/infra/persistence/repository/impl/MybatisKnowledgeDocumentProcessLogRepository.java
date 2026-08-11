package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentProcessLogEntity;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentProcessLogStatus;
import com.clipforge.infra.persistence.mapper.KnowledgeDocumentProcessLogMapper;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentProcessLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisKnowledgeDocumentProcessLogRepository（知识文档处理日志仓储实现）。
 *
 * <p>该实现只在 db Profile（数据库配置环境）下启用，通过 MyBatis-Plus 访问处理日志表。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisKnowledgeDocumentProcessLogRepository implements KnowledgeDocumentProcessLogRepository {

    /** processLogMapper（处理日志映射器）负责执行处理日志表 CRUD（增删改查）。 */
    private final KnowledgeDocumentProcessLogMapper processLogMapper;

    @Override
    public KnowledgeDocumentProcessLogEntity save(KnowledgeDocumentProcessLogEntity entity) {
        processLogMapper.insert(entity);
        return entity;
    }

    @Override
    public Optional<KnowledgeDocumentProcessLogEntity> findByProcessLogId(String processLogId) {
        LambdaQueryWrapper<KnowledgeDocumentProcessLogEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentProcessLogEntity>()
                .eq(KnowledgeDocumentProcessLogEntity::getProcessLogId, processLogId)
                .eq(KnowledgeDocumentProcessLogEntity::getDeleted, 0);
        return Optional.ofNullable(processLogMapper.selectOne(queryWrapper));
    }

    @Override
    public Optional<KnowledgeDocumentProcessLogEntity> findLatestByDocumentId(String documentId) {
        LambdaQueryWrapper<KnowledgeDocumentProcessLogEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentProcessLogEntity>()
                .eq(KnowledgeDocumentProcessLogEntity::getDocumentId, documentId)
                .eq(KnowledgeDocumentProcessLogEntity::getDeleted, 0)
                .orderByDesc(KnowledgeDocumentProcessLogEntity::getStartTime)
                .orderByDesc(KnowledgeDocumentProcessLogEntity::getCreateTime)
                .last("limit 1");
        return Optional.ofNullable(processLogMapper.selectOne(queryWrapper));
    }

    @Override
    public List<KnowledgeDocumentProcessLogEntity> listByDocumentId(String documentId) {
        LambdaQueryWrapper<KnowledgeDocumentProcessLogEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentProcessLogEntity>()
                .eq(KnowledgeDocumentProcessLogEntity::getDocumentId, documentId)
                .eq(KnowledgeDocumentProcessLogEntity::getDeleted, 0)
                .orderByDesc(KnowledgeDocumentProcessLogEntity::getStartTime)
                .orderByDesc(KnowledgeDocumentProcessLogEntity::getCreateTime);
        return processLogMapper.selectList(queryWrapper);
    }

    @Override
    public List<KnowledgeDocumentProcessLogEntity> listRunningBefore(LocalDateTime beforeTime, int limit) {
        LambdaQueryWrapper<KnowledgeDocumentProcessLogEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentProcessLogEntity>()
                .eq(KnowledgeDocumentProcessLogEntity::getStatus, KnowledgeDocumentProcessLogStatus.RUNNING.value())
                .eq(KnowledgeDocumentProcessLogEntity::getDeleted, 0)
                .lt(KnowledgeDocumentProcessLogEntity::getStartTime, beforeTime)
                .orderByAsc(KnowledgeDocumentProcessLogEntity::getStartTime)
                .last("limit " + Math.max(1, limit));
        return processLogMapper.selectList(queryWrapper);
    }

    @Override
    public long countByDocumentIdAndTriggerType(String documentId, String triggerType) {
        LambdaQueryWrapper<KnowledgeDocumentProcessLogEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeDocumentProcessLogEntity>()
                .eq(KnowledgeDocumentProcessLogEntity::getDocumentId, documentId)
                .eq(KnowledgeDocumentProcessLogEntity::getTriggerType, triggerType)
                .eq(KnowledgeDocumentProcessLogEntity::getDeleted, 0);
        return processLogMapper.selectCount(queryWrapper);
    }

    @Override
    public KnowledgeDocumentProcessLogEntity markSuccess(String processLogId, String phase, Long chunkDurationMs, Long embeddingDurationMs, Long totalDurationMs, Integer chunkCount) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentProcessLogEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentProcessLogEntity>()
                .set(KnowledgeDocumentProcessLogEntity::getStatus, KnowledgeDocumentProcessLogStatus.SUCCESS.value())
                .set(KnowledgeDocumentProcessLogEntity::getPhase, phase)
                .set(KnowledgeDocumentProcessLogEntity::getChunkDurationMs, chunkDurationMs)
                .set(KnowledgeDocumentProcessLogEntity::getEmbeddingDurationMs, embeddingDurationMs)
                .set(KnowledgeDocumentProcessLogEntity::getPersistDurationMs, null)
                .set(KnowledgeDocumentProcessLogEntity::getTotalDurationMs, totalDurationMs)
                .set(KnowledgeDocumentProcessLogEntity::getChunkCount, chunkCount)
                .set(KnowledgeDocumentProcessLogEntity::getErrorCode, null)
                .set(KnowledgeDocumentProcessLogEntity::getErrorMessage, null)
                .set(KnowledgeDocumentProcessLogEntity::getEndTime, now)
                .eq(KnowledgeDocumentProcessLogEntity::getProcessLogId, processLogId)
                .eq(KnowledgeDocumentProcessLogEntity::getDeleted, 0);
        processLogMapper.update(null, updateWrapper);
        return findByProcessLogId(processLogId).orElseThrow();
    }

    @Override
    public KnowledgeDocumentProcessLogEntity markFailed(String processLogId, String phase, Long chunkDurationMs, Long embeddingDurationMs, Long totalDurationMs, String errorCode, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<KnowledgeDocumentProcessLogEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeDocumentProcessLogEntity>()
                .set(KnowledgeDocumentProcessLogEntity::getStatus, KnowledgeDocumentProcessLogStatus.FAILED.value())
                .set(KnowledgeDocumentProcessLogEntity::getPhase, phase)
                .set(KnowledgeDocumentProcessLogEntity::getChunkDurationMs, chunkDurationMs)
                .set(KnowledgeDocumentProcessLogEntity::getEmbeddingDurationMs, embeddingDurationMs)
                .set(KnowledgeDocumentProcessLogEntity::getPersistDurationMs, null)
                .set(KnowledgeDocumentProcessLogEntity::getTotalDurationMs, totalDurationMs)
                .set(KnowledgeDocumentProcessLogEntity::getErrorCode, shorten(errorCode, 64))
                .set(KnowledgeDocumentProcessLogEntity::getErrorMessage, shorten(errorMessage, 1000))
                .set(KnowledgeDocumentProcessLogEntity::getEndTime, now)
                .eq(KnowledgeDocumentProcessLogEntity::getProcessLogId, processLogId)
                .eq(KnowledgeDocumentProcessLogEntity::getDeleted, 0);
        processLogMapper.update(null, updateWrapper);
        return findByProcessLogId(processLogId).orElseThrow();
    }

    /**
     * shorten（截断）用于避免过长文本写入数据库字段。
     *
     * @param value 原始文本
     * @param limit 最大长度
     * @return 截断后的文本
     */
    private String shorten(String value, int limit) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
