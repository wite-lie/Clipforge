package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.CreationPlanVersionEntity;
import com.clipforge.infra.persistence.mapper.CreationPlanVersionMapper;
import com.clipforge.infra.persistence.repository.CreationPlanVersionRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisCreationPlanVersionRepository（MyBatis 创作方案版本仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，负责创作方案版本表读写。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisCreationPlanVersionRepository implements CreationPlanVersionRepository {

    /** creationPlanVersionMapper（创作方案版本映射器）负责执行数据库 CRUD（增删改查）。 */
    private final CreationPlanVersionMapper creationPlanVersionMapper;

    @Override
    public CreationPlanVersionEntity save(CreationPlanVersionEntity entity) {
        creationPlanVersionMapper.insert(entity);
        return entity;
    }

    @Override
    public int nextVersionNo(String runId) {
        LambdaQueryWrapper<CreationPlanVersionEntity> queryWrapper = new LambdaQueryWrapper<CreationPlanVersionEntity>()
                .eq(CreationPlanVersionEntity::getRunId, runId)
                .eq(CreationPlanVersionEntity::getDeleted, 0);
        return Math.toIntExact(creationPlanVersionMapper.selectCount(queryWrapper)) + 1;
    }

    @Override
    public void markFinal(String versionId) {
        CreationPlanVersionEntity version = creationPlanVersionMapper.selectOne(new LambdaQueryWrapper<CreationPlanVersionEntity>()
                .eq(CreationPlanVersionEntity::getVersionId, versionId)
                .eq(CreationPlanVersionEntity::getDeleted, 0));
        if (version == null) {
            return;
        }
        LambdaUpdateWrapper<CreationPlanVersionEntity> clearWrapper = new LambdaUpdateWrapper<CreationPlanVersionEntity>()
                .eq(CreationPlanVersionEntity::getRunId, version.getRunId())
                .eq(CreationPlanVersionEntity::getDeleted, 0)
                .set(CreationPlanVersionEntity::getFinalVersion, false)
                .set(CreationPlanVersionEntity::getUpdateTime, LocalDateTime.now());
        creationPlanVersionMapper.update(null, clearWrapper);
        LambdaUpdateWrapper<CreationPlanVersionEntity> markWrapper = new LambdaUpdateWrapper<CreationPlanVersionEntity>()
                .eq(CreationPlanVersionEntity::getVersionId, versionId)
                .eq(CreationPlanVersionEntity::getDeleted, 0)
                .set(CreationPlanVersionEntity::getFinalVersion, true)
                .set(CreationPlanVersionEntity::getUpdateTime, LocalDateTime.now());
        creationPlanVersionMapper.update(null, markWrapper);
    }

    @Override
    public List<CreationPlanVersionEntity> listByRunId(String runId) {
        LambdaQueryWrapper<CreationPlanVersionEntity> queryWrapper = new LambdaQueryWrapper<CreationPlanVersionEntity>()
                .eq(CreationPlanVersionEntity::getRunId, runId)
                .eq(CreationPlanVersionEntity::getDeleted, 0)
                .orderByAsc(CreationPlanVersionEntity::getVersionNo)
                .orderByAsc(CreationPlanVersionEntity::getCreateTime);
        return creationPlanVersionMapper.selectList(queryWrapper);
    }
}
