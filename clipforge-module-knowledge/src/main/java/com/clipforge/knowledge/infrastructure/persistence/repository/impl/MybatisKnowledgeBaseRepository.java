package com.clipforge.knowledge.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeBaseEntity;
import com.clipforge.knowledge.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeBaseRepository;
import com.clipforge.knowledge.query.KnowledgeBaseQueryCondition;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisKnowledgeBaseRepository（MyBatis 知识库仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问 cf_knowledge_base（知识库表）。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisKnowledgeBaseRepository implements KnowledgeBaseRepository {

    /** knowledgeBaseMapper（知识库映射器）负责执行知识库表 CRUD（增删改查）。 */
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * save（保存）用于写入知识库实体。
     *
     * @param entity 知识库实体
     * @return 已保存知识库实体
     */
    @Override
    public KnowledgeBaseEntity save(KnowledgeBaseEntity entity) {
        // 插入知识库实体。
        knowledgeBaseMapper.insert(entity);
        // 返回已保存实体。
        return entity;
    }

    /**
     * findByKnowledgeBaseId（按知识库编号查询）用于读取知识库详情。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 可能存在的知识库实体
     */
    @Override
    public Optional<KnowledgeBaseEntity> findByKnowledgeBaseId(String knowledgeBaseId) {
        // 创建按知识库编号查询且排除逻辑删除数据的条件。
        LambdaQueryWrapper<KnowledgeBaseEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeBaseEntity>()
                // knowledgeBaseId（知识库编号）必须匹配。
                .eq(KnowledgeBaseEntity::getKnowledgeBaseId, knowledgeBaseId)
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeBaseEntity::getDeleted, 0);
        // 执行查询并包装为 Optional（可选值）。
        return Optional.ofNullable(knowledgeBaseMapper.selectOne(queryWrapper));
    }

    /**
     * listByCondition（按条件查询列表）用于按多个可选条件筛选知识库。
     *
     * @param condition 查询条件
     * @return 知识库实体列表
     */
    @Override
    public List<KnowledgeBaseEntity> listByCondition(KnowledgeBaseQueryCondition condition) {
        // 创建 MyBatis-Plus（MyBatis 增强工具）查询条件包装器。
        LambdaQueryWrapper<KnowledgeBaseEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeBaseEntity>()
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeBaseEntity::getDeleted, 0)
                // 按创建时间倒序排列，方便优先看到最近创建的知识库。
                .orderByDesc(KnowledgeBaseEntity::getCreateTime);
        // 如果传入 userId（用户编号），则按用户筛选。
        if (hasText(condition.getUserId())) {
            // 添加用户编号相等条件。
            queryWrapper.eq(KnowledgeBaseEntity::getUserId, condition.getUserId());
        }
        // 如果传入 category（分类），则按分类筛选。
        if (hasText(condition.getCategory())) {
            // 添加分类相等条件。
            queryWrapper.eq(KnowledgeBaseEntity::getCategory, condition.getCategory());
        }
        // 如果传入 defaultPlatform（默认平台），则按默认平台筛选。
        if (hasText(condition.getDefaultPlatform())) {
            // 添加默认平台相等条件。
            queryWrapper.eq(KnowledgeBaseEntity::getDefaultPlatform, condition.getDefaultPlatform());
        }
        // 如果传入 status（状态），则按状态筛选。
        if (hasText(condition.getStatus())) {
            // 添加状态相等条件。
            queryWrapper.eq(KnowledgeBaseEntity::getStatus, condition.getStatus());
        }
        // 如果传入 enabled（是否启用），则按启用状态筛选。
        if (condition.getEnabled() != null) {
            // 添加启用状态相等条件。
            queryWrapper.eq(KnowledgeBaseEntity::getEnabled, condition.getEnabled());
        }
        // 执行列表查询并返回。
        return knowledgeBaseMapper.selectList(queryWrapper);
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
