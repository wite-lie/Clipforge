package com.clipforge.memory.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.memory.infrastructure.persistence.entity.ProjectMemoryEntity;
import com.clipforge.memory.infrastructure.persistence.enums.MemoryStatus;
import com.clipforge.memory.infrastructure.persistence.mapper.ProjectMemoryMapper;
import com.clipforge.memory.infrastructure.persistence.repository.ProjectMemoryRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisProjectMemoryRepository（MyBatis 项目记忆仓储实现）。
 *
 * <p>该实现仅在 db（数据库）Profile（配置环境）下启用。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisProjectMemoryRepository implements ProjectMemoryRepository {

    /** projectMemoryMapper（项目记忆映射器）负责访问项目记忆表。 */
    private final ProjectMemoryMapper projectMemoryMapper;

    @Override
    public ProjectMemoryEntity save(ProjectMemoryEntity entity) {
        projectMemoryMapper.insert(entity);
        return entity;
    }

    @Override
    public Optional<ProjectMemoryEntity> findByMemoryId(String memoryId) {
        LambdaQueryWrapper<ProjectMemoryEntity> queryWrapper = new LambdaQueryWrapper<ProjectMemoryEntity>()
                .eq(ProjectMemoryEntity::getMemoryId, memoryId)
                .eq(ProjectMemoryEntity::getDeleted, 0);
        return Optional.ofNullable(projectMemoryMapper.selectOne(queryWrapper));
    }

    @Override
    public Optional<ProjectMemoryEntity> findActiveByProjectId(String projectId) {
        LambdaQueryWrapper<ProjectMemoryEntity> queryWrapper = new LambdaQueryWrapper<ProjectMemoryEntity>()
                .eq(ProjectMemoryEntity::getProjectId, projectId)
                .eq(ProjectMemoryEntity::getStatus, MemoryStatus.ACTIVE.value())
                .eq(ProjectMemoryEntity::getDeleted, 0)
                .orderByDesc(ProjectMemoryEntity::getUpdateTime)
                .last("limit 1");
        return Optional.ofNullable(projectMemoryMapper.selectOne(queryWrapper));
    }

    @Override
    public boolean disableByMemoryId(String memoryId) {
        LambdaUpdateWrapper<ProjectMemoryEntity> updateWrapper = new LambdaUpdateWrapper<ProjectMemoryEntity>()
                .set(ProjectMemoryEntity::getStatus, MemoryStatus.DISABLED.value())
                .set(ProjectMemoryEntity::getUpdateTime, LocalDateTime.now())
                .eq(ProjectMemoryEntity::getMemoryId, memoryId)
                .eq(ProjectMemoryEntity::getDeleted, 0);
        return projectMemoryMapper.update(null, updateWrapper) > 0;
    }
}
