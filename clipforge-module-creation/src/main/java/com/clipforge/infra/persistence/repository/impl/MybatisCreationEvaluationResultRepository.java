package com.clipforge.memory.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.memory.infrastructure.persistence.entity.CreationEvaluationResultEntity;
import com.clipforge.memory.infrastructure.persistence.mapper.CreationEvaluationResultMapper;
import com.clipforge.memory.infrastructure.persistence.repository.CreationEvaluationResultRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisCreationEvaluationResultRepository（MyBatis 创作评估结果仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，负责评估结果表读写。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisCreationEvaluationResultRepository implements CreationEvaluationResultRepository {

    /** creationEvaluationResultMapper（创作评估结果映射器）负责执行数据库 CRUD（增删改查）。 */
    private final CreationEvaluationResultMapper creationEvaluationResultMapper;

    @Override
    public CreationEvaluationResultEntity save(CreationEvaluationResultEntity entity) {
        creationEvaluationResultMapper.insert(entity);
        return entity;
    }

    @Override
    public List<CreationEvaluationResultEntity> listByRunId(String runId) {
        LambdaQueryWrapper<CreationEvaluationResultEntity> queryWrapper = new LambdaQueryWrapper<CreationEvaluationResultEntity>()
                .eq(CreationEvaluationResultEntity::getRunId, runId)
                .eq(CreationEvaluationResultEntity::getDeleted, 0)
                .orderByAsc(CreationEvaluationResultEntity::getCreateTime);
        return creationEvaluationResultMapper.selectList(queryWrapper);
    }
}
