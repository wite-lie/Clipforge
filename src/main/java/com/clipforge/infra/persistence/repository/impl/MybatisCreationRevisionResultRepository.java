package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.infra.persistence.entity.CreationRevisionResultEntity;
import com.clipforge.infra.persistence.mapper.CreationRevisionResultMapper;
import com.clipforge.infra.persistence.repository.CreationRevisionResultRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisCreationRevisionResultRepository（MyBatis 创作修正结果仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，负责修正结果表读写。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisCreationRevisionResultRepository implements CreationRevisionResultRepository {

    /** creationRevisionResultMapper（创作修正结果映射器）负责执行数据库 CRUD（增删改查）。 */
    private final CreationRevisionResultMapper creationRevisionResultMapper;

    @Override
    public CreationRevisionResultEntity save(CreationRevisionResultEntity entity) {
        creationRevisionResultMapper.insert(entity);
        return entity;
    }

    @Override
    public List<CreationRevisionResultEntity> listByRunId(String runId) {
        LambdaQueryWrapper<CreationRevisionResultEntity> queryWrapper = new LambdaQueryWrapper<CreationRevisionResultEntity>()
                .eq(CreationRevisionResultEntity::getRunId, runId)
                .eq(CreationRevisionResultEntity::getDeleted, 0)
                .orderByAsc(CreationRevisionResultEntity::getCreateTime);
        return creationRevisionResultMapper.selectList(queryWrapper);
    }
}
