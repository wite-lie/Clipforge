package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.MaterialEntity;
import com.clipforge.infra.persistence.mapper.MaterialMapper;
import com.clipforge.infra.persistence.repository.MaterialRepository;
import com.clipforge.material.MaterialQueryCondition;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisMaterialRepository（MyBatis 素材仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问素材表。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisMaterialRepository implements MaterialRepository {

    /** materialMapper（素材映射器）负责执行素材表 CRUD（增删改查）。 */
    private final MaterialMapper materialMapper;

    /**
     * save（保存）用于写入素材元数据。
     *
     * @param entity 素材实体
     * @return 已保存素材实体
     */
    @Override
    public MaterialEntity save(MaterialEntity entity) {
        // 插入素材实体。
        materialMapper.insert(entity);
        // 返回已保存实体。
        return entity;
    }

    /**
     * findByMaterialId（按素材编号查询）用于读取素材详情。
     *
     * @param materialId 素材编号
     * @return 可能存在的素材实体
     */
    @Override
    public Optional<MaterialEntity> findByMaterialId(String materialId) {
        // 创建查询条件。
        LambdaQueryWrapper<MaterialEntity> queryWrapper = new LambdaQueryWrapper<MaterialEntity>()
                // materialId（素材编号）必须匹配。
                .eq(MaterialEntity::getMaterialId, materialId)
                // deleted（逻辑删除）必须为正常。
                .eq(MaterialEntity::getDeleted, 0);
        // 执行查询并包装为 Optional（可选值）。
        return Optional.ofNullable(materialMapper.selectOne(queryWrapper));
    }

    /**
     * listByCondition（按条件查询列表）用于按用户、项目、会话、运行任务和素材类型筛选素材。
     *
     * @param condition 素材查询条件
     * @return 素材实体列表
     */
    @Override
    public List<MaterialEntity> listByCondition(MaterialQueryCondition condition) {
        // 创建 MyBatis-Plus（MyBatis 增强工具）查询条件包装器。
        LambdaQueryWrapper<MaterialEntity> queryWrapper = new LambdaQueryWrapper<MaterialEntity>()
                // deleted（逻辑删除）必须为正常。
                .eq(MaterialEntity::getDeleted, 0)
                // 按创建时间倒序排列，方便前端优先看到最近上传的素材。
                .orderByDesc(MaterialEntity::getCreateTime);
        // 如果传入 userId（用户编号），则按用户筛选。
        if (hasText(condition.getUserId())) {
            // 添加用户编号相等条件。
            queryWrapper.eq(MaterialEntity::getUserId, condition.getUserId());
        }
        // 如果传入 projectId（项目编号），则按项目筛选。
        if (hasText(condition.getProjectId())) {
            // 添加项目编号相等条件。
            queryWrapper.eq(MaterialEntity::getProjectId, condition.getProjectId());
        }
        // 如果传入 conversationId（会话编号），则按会话筛选。
        if (hasText(condition.getConversationId())) {
            // 添加会话编号相等条件。
            queryWrapper.eq(MaterialEntity::getConversationId, condition.getConversationId());
        }
        // 如果传入 runId（运行编号），则按运行任务筛选。
        if (hasText(condition.getRunId())) {
            // 添加运行编号相等条件。
            queryWrapper.eq(MaterialEntity::getRunId, condition.getRunId());
        }
        // 如果传入 materialType（素材类型），则按素材类型筛选。
        if (hasText(condition.getMaterialType())) {
            // 添加素材类型相等条件。
            queryWrapper.eq(MaterialEntity::getMaterialType, condition.getMaterialType());
        }
        // 执行列表查询并返回。
        return materialMapper.selectList(queryWrapper);
    }

    /**
     * listByMaterialIds（按素材编号列表查询）用于读取用户明确选择的素材集合。
     *
     * @param materialIds 素材编号列表
     * @param materialType 素材类型
     * @return 素材实体列表
     */
    @Override
    public List<MaterialEntity> listByMaterialIds(List<String> materialIds, String materialType) {
        // 如果素材编号列表为空，则直接返回空列表，避免生成无意义 SQL（结构化查询语言）。
        if (materialIds == null || materialIds.isEmpty()) {
            // 返回空素材列表。
            return List.of();
        }
        // 创建 MyBatis-Plus（MyBatis 增强工具）查询条件包装器。
        LambdaQueryWrapper<MaterialEntity> queryWrapper = new LambdaQueryWrapper<MaterialEntity>()
                // deleted（逻辑删除）必须为正常。
                .eq(MaterialEntity::getDeleted, 0)
                // materialId（素材编号）必须在用户选择列表中。
                .in(MaterialEntity::getMaterialId, materialIds);
        // 如果传入素材类型，则限定素材类型。
        if (hasText(materialType)) {
            // 添加素材类型条件。
            queryWrapper.eq(MaterialEntity::getMaterialType, materialType);
        }
        // 执行列表查询。
        return materialMapper.selectList(queryWrapper);
    }

    /**
     * logicalDeleteByMaterialIds（按素材编号列表逻辑删除）用于清理系统派生素材。
     *
     * @param materialIds 素材编号列表
     */
    @Override
    public void logicalDeleteByMaterialIds(List<String> materialIds) {
        // 如果素材编号列表为空，则直接返回，避免生成无意义 SQL（结构化查询语言）。
        if (materialIds == null || materialIds.isEmpty()) {
            // 结束方法。
            return;
        }
        // 创建更新条件。
        LambdaUpdateWrapper<MaterialEntity> updateWrapper = new LambdaUpdateWrapper<MaterialEntity>()
                // 设置逻辑删除标记。
                .set(MaterialEntity::getDeleted, 1)
                // 更新时间。
                .set(MaterialEntity::getUpdateTime, LocalDateTime.now())
                // 限定素材编号列表。
                .in(MaterialEntity::getMaterialId, materialIds)
                // 只处理未删除记录。
                .eq(MaterialEntity::getDeleted, 0);
        // 执行逻辑删除。
        materialMapper.update(null, updateWrapper);
    }

    /**
     * hasText（是否有文本）用于判断字符串是否非空且不是空白。
     *
     * @param value 原始字符串
     * @return 有实际文本时返回 true（真）
     */
    private boolean hasText(String value) {
        // 返回字符串非空且去除空白后仍有内容的判断结果。
        return value != null && !value.isBlank();
    }
}
