package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.CreationHistoryMemoryEntity;
import com.clipforge.infra.persistence.enums.MemoryStatus;
import com.clipforge.infra.persistence.mapper.CreationHistoryMemoryMapper;
import com.clipforge.infra.persistence.repository.CreationHistoryMemoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisCreationHistoryMemoryRepository（MyBatis 创作历史记忆仓储实现）。
 *
 * <p>该实现仅在 db（数据库）Profile（配置环境）下启用。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisCreationHistoryMemoryRepository implements CreationHistoryMemoryRepository {

    /** creationHistoryMemoryMapper（创作历史记忆映射器）负责访问创作历史记忆表。 */
    private final CreationHistoryMemoryMapper creationHistoryMemoryMapper;

    @Override
    public CreationHistoryMemoryEntity save(CreationHistoryMemoryEntity entity) {
        creationHistoryMemoryMapper.insert(entity);
        return entity;
    }

    @Override
    public Optional<CreationHistoryMemoryEntity> findByMemoryId(String memoryId) {
        LambdaQueryWrapper<CreationHistoryMemoryEntity> queryWrapper = new LambdaQueryWrapper<CreationHistoryMemoryEntity>()
                .eq(CreationHistoryMemoryEntity::getMemoryId, memoryId)
                .eq(CreationHistoryMemoryEntity::getDeleted, 0);
        return Optional.ofNullable(creationHistoryMemoryMapper.selectOne(queryWrapper));
    }

    @Override
    public List<CreationHistoryMemoryEntity> listRecentActiveByUserId(String userId, int limit) {
        LambdaQueryWrapper<CreationHistoryMemoryEntity> queryWrapper = new LambdaQueryWrapper<CreationHistoryMemoryEntity>()
                .eq(CreationHistoryMemoryEntity::getUserId, userId)
                .eq(CreationHistoryMemoryEntity::getStatus, MemoryStatus.ACTIVE.value())
                .eq(CreationHistoryMemoryEntity::getDeleted, 0)
                .orderByDesc(CreationHistoryMemoryEntity::getCreateTime)
                .last("limit " + Math.max(limit, 1));
        return creationHistoryMemoryMapper.selectList(queryWrapper);
    }

    @Override
    public List<CreationHistoryMemoryEntity> listActiveByProjectId(String projectId, String platform, int limit) {
        // 构造按项目查询启用历史记忆的条件。
        LambdaQueryWrapper<CreationHistoryMemoryEntity> queryWrapper = new LambdaQueryWrapper<CreationHistoryMemoryEntity>()
                // 限定项目编号。
                .eq(CreationHistoryMemoryEntity::getProjectId, projectId)
                // 限定启用状态。
                .eq(CreationHistoryMemoryEntity::getStatus, MemoryStatus.ACTIVE.value())
                // 排除逻辑删除数据。
                .eq(CreationHistoryMemoryEntity::getDeleted, 0)
                // 优先返回高分方案。
                .orderByDesc(CreationHistoryMemoryEntity::getScore)
                // 同分时返回较新的记忆。
                .orderByDesc(CreationHistoryMemoryEntity::getCreateTime)
                // 限制返回数量。
                .last("limit " + Math.max(limit, 1));
        // 如果传入平台，则继续按平台过滤。
        if (platform != null && !platform.isBlank()) {
            // 添加平台条件。
            queryWrapper.eq(CreationHistoryMemoryEntity::getPlatform, platform);
        }
        // 执行查询。
        return creationHistoryMemoryMapper.selectList(queryWrapper);
    }

    @Override
    public List<CreationHistoryMemoryEntity> listHighScoreByProjectAndPlatform(String projectId, String platform, int minScore, int limit) {
        LambdaQueryWrapper<CreationHistoryMemoryEntity> queryWrapper = new LambdaQueryWrapper<CreationHistoryMemoryEntity>()
                .eq(CreationHistoryMemoryEntity::getProjectId, projectId)
                .eq(CreationHistoryMemoryEntity::getPlatform, platform)
                .ge(CreationHistoryMemoryEntity::getScore, minScore)
                .eq(CreationHistoryMemoryEntity::getStatus, MemoryStatus.ACTIVE.value())
                .eq(CreationHistoryMemoryEntity::getDeleted, 0)
                .orderByDesc(CreationHistoryMemoryEntity::getScore)
                .orderByDesc(CreationHistoryMemoryEntity::getCreateTime)
                .last("limit " + Math.max(limit, 1));
        return creationHistoryMemoryMapper.selectList(queryWrapper);
    }

    @Override
    public boolean disableByMemoryId(String memoryId) {
        LambdaUpdateWrapper<CreationHistoryMemoryEntity> updateWrapper = new LambdaUpdateWrapper<CreationHistoryMemoryEntity>()
                .set(CreationHistoryMemoryEntity::getStatus, MemoryStatus.DISABLED.value())
                .set(CreationHistoryMemoryEntity::getUpdateTime, LocalDateTime.now())
                .eq(CreationHistoryMemoryEntity::getMemoryId, memoryId)
                .eq(CreationHistoryMemoryEntity::getDeleted, 0);
        return creationHistoryMemoryMapper.update(null, updateWrapper) > 0;
    }
}
