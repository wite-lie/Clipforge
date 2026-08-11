package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.infra.persistence.entity.MaterialUnderstandingEntity;
import com.clipforge.infra.persistence.mapper.MaterialUnderstandingMapper;
import com.clipforge.infra.persistence.repository.MaterialUnderstandingRepository;
import com.clipforge.material.understanding.MaterialUnderstandingStatus;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisMaterialUnderstandingRepository（MyBatis 素材理解结果仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问素材理解结果表。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisMaterialUnderstandingRepository implements MaterialUnderstandingRepository {

    /** materialUnderstandingMapper（素材理解结果映射器）负责执行素材理解结果表 CRUD（增删改查）。 */
    private final MaterialUnderstandingMapper materialUnderstandingMapper;

    /**
     * save（保存）用于写入素材理解结果。
     *
     * @param entity 素材理解结果实体
     * @return 已保存实体
     */
    @Override
    public MaterialUnderstandingEntity save(MaterialUnderstandingEntity entity) {
        // 插入素材理解结果实体。
        materialUnderstandingMapper.insert(entity);
        // 返回已保存实体。
        return entity;
    }

    /**
     * findReusable（查询可复用结果）用于按素材、理解类型、模型、提示词版本和内容哈希查询成功结果。
     *
     * @param materialId 素材编号
     * @param understandingType 理解类型
     * @param modelName 模型名称
     * @param promptVersion 提示词版本
     * @param contentHash 内容哈希
     * @return 可能存在的素材理解结果
     */
    @Override
    public Optional<MaterialUnderstandingEntity> findReusable(String materialId, String understandingType, String modelName, String promptVersion, String contentHash) {
        // 创建查询条件。
        LambdaQueryWrapper<MaterialUnderstandingEntity> queryWrapper = new LambdaQueryWrapper<MaterialUnderstandingEntity>()
                // 素材编号必须匹配。
                .eq(MaterialUnderstandingEntity::getMaterialId, materialId)
                // 理解类型必须匹配。
                .eq(MaterialUnderstandingEntity::getUnderstandingType, understandingType)
                // 模型名称必须匹配。
                .eq(MaterialUnderstandingEntity::getModelName, modelName)
                // 提示词版本必须匹配。
                .eq(MaterialUnderstandingEntity::getPromptVersion, promptVersion)
                // 内容哈希必须匹配。
                .eq(MaterialUnderstandingEntity::getContentHash, contentHash)
                // 只复用成功结果。
                .eq(MaterialUnderstandingEntity::getStatus, MaterialUnderstandingStatus.SUCCEEDED.value())
                // 只查询未删除记录。
                .eq(MaterialUnderstandingEntity::getDeleted, 0)
                // 按创建时间倒序。
                .orderByDesc(MaterialUnderstandingEntity::getCreateTime)
                // 只查询一条。
                .last("LIMIT 1");
        // 执行查询并包装为 Optional（可选值）。
        return Optional.ofNullable(materialUnderstandingMapper.selectOne(queryWrapper));
    }

    /**
     * findLatestSucceeded（查询最近成功结果）用于创作 Workflow（工作流）读取已保存的素材理解结果。
     *
     * @param materialId 素材编号
     * @param understandingType 理解类型
     * @return 可能存在的素材理解结果
     */
    @Override
    public Optional<MaterialUnderstandingEntity> findLatestSucceeded(String materialId, String understandingType) {
        // 创建查询条件。
        LambdaQueryWrapper<MaterialUnderstandingEntity> queryWrapper = new LambdaQueryWrapper<MaterialUnderstandingEntity>()
                // 素材编号必须匹配。
                .eq(MaterialUnderstandingEntity::getMaterialId, materialId)
                // 理解类型必须匹配。
                .eq(MaterialUnderstandingEntity::getUnderstandingType, understandingType)
                // 只查询成功结果。
                .eq(MaterialUnderstandingEntity::getStatus, MaterialUnderstandingStatus.SUCCEEDED.value())
                // 只查询未删除记录。
                .eq(MaterialUnderstandingEntity::getDeleted, 0)
                // 按创建时间倒序，取最近一次结果。
                .orderByDesc(MaterialUnderstandingEntity::getCreateTime)
                // 只查询一条。
                .last("LIMIT 1");
        // 执行查询并包装为 Optional（可选值）。
        return Optional.ofNullable(materialUnderstandingMapper.selectOne(queryWrapper));
    }
}
