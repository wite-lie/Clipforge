package com.clipforge.memory.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.memory.infrastructure.persistence.entity.CreationExportEntity;
import com.clipforge.memory.infrastructure.persistence.mapper.CreationExportMapper;
import com.clipforge.memory.infrastructure.persistence.repository.CreationExportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MybatisCreationExportRepository（MyBatis 创作导出仓储实现）。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisCreationExportRepository implements CreationExportRepository {

    /** creationExportMapper（创作导出映射器）负责执行数据库 CRUD（增删改查）。 */
    private final CreationExportMapper creationExportMapper;

    @Override
    public CreationExportEntity save(CreationExportEntity entity) {
        creationExportMapper.insert(entity);
        return entity;
    }

    @Override
    public Optional<CreationExportEntity> findByExportId(String exportId) {
        LambdaQueryWrapper<CreationExportEntity> queryWrapper = new LambdaQueryWrapper<CreationExportEntity>()
                .eq(CreationExportEntity::getExportId, exportId)
                .eq(CreationExportEntity::getDeleted, 0);
        return Optional.ofNullable(creationExportMapper.selectOne(queryWrapper));
    }

    @Override
    public Optional<CreationExportEntity> findLatestSucceededByRunId(String runId) {
        LambdaQueryWrapper<CreationExportEntity> queryWrapper = new LambdaQueryWrapper<CreationExportEntity>()
                .eq(CreationExportEntity::getRunId, runId)
                .eq(CreationExportEntity::getExportFormat, "MARKDOWN")
                .eq(CreationExportEntity::getStatus, "SUCCEEDED")
                .eq(CreationExportEntity::getDeleted, 0)
                .orderByDesc(CreationExportEntity::getCreateTime)
                .last("LIMIT 1");
        return Optional.ofNullable(creationExportMapper.selectOne(queryWrapper));
    }
}
