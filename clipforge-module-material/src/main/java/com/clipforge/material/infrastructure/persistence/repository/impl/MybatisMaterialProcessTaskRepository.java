package com.clipforge.material.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.material.infrastructure.persistence.entity.MaterialProcessTaskEntity;
import com.clipforge.material.infrastructure.persistence.mapper.MaterialProcessTaskMapper;
import com.clipforge.material.infrastructure.persistence.repository.MaterialProcessTaskRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisMaterialProcessTaskRepository（MyBatis 素材处理任务仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问素材处理任务表。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisMaterialProcessTaskRepository implements MaterialProcessTaskRepository {

    /** materialProcessTaskMapper（素材处理任务映射器）负责执行素材处理任务表 CRUD（增删改查）。 */
    private final MaterialProcessTaskMapper materialProcessTaskMapper;

    /**
     * save（保存）用于写入素材处理任务。
     *
     * @param entity 素材处理任务实体
     * @return 已保存素材处理任务实体
     */
    @Override
    public MaterialProcessTaskEntity save(MaterialProcessTaskEntity entity) {
        // 插入素材处理任务实体。
        materialProcessTaskMapper.insert(entity);
        // 返回已保存实体。
        return entity;
    }

    /**
     * findByProcessTaskId（按处理任务编号查询）用于读取任务状态。
     *
     * @param processTaskId 处理任务编号
     * @return 可能存在的素材处理任务实体
     */
    @Override
    public Optional<MaterialProcessTaskEntity> findByProcessTaskId(String processTaskId) {
        // 创建查询条件。
        LambdaQueryWrapper<MaterialProcessTaskEntity> queryWrapper = new LambdaQueryWrapper<MaterialProcessTaskEntity>()
                // processTaskId（处理任务编号）必须匹配。
                .eq(MaterialProcessTaskEntity::getProcessTaskId, processTaskId)
                // deleted（逻辑删除）必须为正常。
                .eq(MaterialProcessTaskEntity::getDeleted, 0);
        // 执行查询并包装为 Optional（可选值）。
        return Optional.ofNullable(materialProcessTaskMapper.selectOne(queryWrapper));
    }

    /**
     * findByParentProcessTaskId（按父任务编号查询子任务）用于读取视频父任务下的所有子任务。
     *
     * @param parentProcessTaskId 父处理任务编号
     * @return 子任务列表
     */
    @Override
    public List<MaterialProcessTaskEntity> findByParentProcessTaskId(String parentProcessTaskId) {
        // 创建查询条件。
        LambdaQueryWrapper<MaterialProcessTaskEntity> queryWrapper = new LambdaQueryWrapper<MaterialProcessTaskEntity>()
                .eq(MaterialProcessTaskEntity::getParentProcessTaskId, parentProcessTaskId)
                .eq(MaterialProcessTaskEntity::getDeleted, 0)
                .orderByAsc(MaterialProcessTaskEntity::getCreateTime);
        // 执行查询。
        return materialProcessTaskMapper.selectList(queryWrapper);
    }

    /**
     * findByParentProcessTaskIdAndSubTaskKey（按父任务编号和子任务键查询）用于幂等读取单个子任务。
     *
     * @param parentProcessTaskId 父处理任务编号
     * @param subTaskKey 子任务键
     * @return 可能存在的子任务
     */
    @Override
    public Optional<MaterialProcessTaskEntity> findByParentProcessTaskIdAndSubTaskKey(String parentProcessTaskId, String subTaskKey) {
        // 创建查询条件。
        LambdaQueryWrapper<MaterialProcessTaskEntity> queryWrapper = new LambdaQueryWrapper<MaterialProcessTaskEntity>()
                .eq(MaterialProcessTaskEntity::getParentProcessTaskId, parentProcessTaskId)
                .eq(MaterialProcessTaskEntity::getSubTaskKey, subTaskKey)
                .eq(MaterialProcessTaskEntity::getDeleted, 0)
                .last("LIMIT 1");
        // 执行查询并包装为 Optional（可选值）。
        return Optional.ofNullable(materialProcessTaskMapper.selectOne(queryWrapper));
    }

    /**
     * findLatestByMaterialId（按素材编号查询最近任务）用于读取某个素材最近一次处理任务。
     *
     * @param materialId 素材编号
     * @return 可能存在的素材处理任务实体
     */
    @Override
    public Optional<MaterialProcessTaskEntity> findLatestByMaterialId(String materialId) {
        // 创建查询条件。
        LambdaQueryWrapper<MaterialProcessTaskEntity> queryWrapper = new LambdaQueryWrapper<MaterialProcessTaskEntity>()
                // materialId（素材编号）必须匹配。
                .eq(MaterialProcessTaskEntity::getMaterialId, materialId)
                // deleted（逻辑删除）必须为正常。
                .eq(MaterialProcessTaskEntity::getDeleted, 0)
                // 按创建时间倒序，取最近一次处理任务。
                .orderByDesc(MaterialProcessTaskEntity::getCreateTime)
                // 只查询一条记录。
                .last("LIMIT 1");
        // 执行查询并包装为 Optional（可选值）。
        return Optional.ofNullable(materialProcessTaskMapper.selectOne(queryWrapper));
    }

    /**
     * update（更新）用于把处理任务最新状态写回数据库。
     *
     * @param entity 素材处理任务实体
     */
    @Override
    public void update(MaterialProcessTaskEntity entity) {
        // 按内部主键更新任务记录，保留创建时间等历史字段。
        materialProcessTaskMapper.updateById(entity);
    }
}
