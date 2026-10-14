package com.clipforge.memory.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.memory.infrastructure.persistence.entity.ConversationEntity;
import com.clipforge.memory.infrastructure.persistence.mapper.ConversationMapper;
import com.clipforge.memory.infrastructure.persistence.repository.ConversationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisConversationRepository（MyBatis 会话仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问会话表。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisConversationRepository implements ConversationRepository {

    /** conversationMapper（会话映射器）负责执行会话表 CRUD（增删改查）。 */
    private final ConversationMapper conversationMapper;

    /**
     * create（创建）用于创建一次会话。
     *
     * @param projectId 项目编号
     * @param userId 用户编号
     * @param title 会话标题
     * @return 已保存会话实体
     */
    @Override
    public ConversationEntity create(String projectId, String userId, String title) {
        // 获取当前时间，统一填充创建时间和更新时间。
        LocalDateTime now = LocalDateTime.now();
        // 使用 Builder（建造者模式）组装会话实体。
        ConversationEntity entity = ConversationEntity.builder()
                // 设置数据库内部主键。
                .id(newId("conversation_pk"))
                // 设置对外会话编号。
                .conversationId(newId("conversation"))
                // 设置项目编号。
                .projectId(projectId)
                // 设置用户编号。
                .userId(userId)
                // 设置会话标题。
                .title(title)
                // 设置会话状态为进行中。
                .status("ACTIVE")
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记为正常。
                .deleted(0)
                // 构建实体。
                .build();
        // 插入数据库。
        conversationMapper.insert(entity);
        // 返回已保存会话实体。
        return entity;
    }

    /**
     * findByConversationId（按会话编号查询）用于读取会话详情。
     *
     * @param conversationId 会话编号
     * @return 可能存在的会话实体
     */
    @Override
    public Optional<ConversationEntity> findByConversationId(String conversationId) {
        // 创建按会话编号查询且排除逻辑删除数据的条件。
        LambdaQueryWrapper<ConversationEntity> queryWrapper = new LambdaQueryWrapper<ConversationEntity>()
                // conversationId（会话编号）必须匹配。
                .eq(ConversationEntity::getConversationId, conversationId)
                // deleted（逻辑删除）必须为正常。
                .eq(ConversationEntity::getDeleted, 0);
        // 执行查询并用 Optional（可选值）包装。
        return Optional.ofNullable(conversationMapper.selectOne(queryWrapper));
    }

    /**
     * listByProjectId（按项目编号查询列表）用于读取项目下的会话列表。
     *
     * @param projectId 项目编号
     * @return 会话实体列表
     */
    @Override
    public List<ConversationEntity> listByProjectId(String projectId) {
        // 创建按项目编号查询、排除逻辑删除并按创建时间倒序排列的条件。
        LambdaQueryWrapper<ConversationEntity> queryWrapper = new LambdaQueryWrapper<ConversationEntity>()
                // projectId（项目编号）必须匹配。
                .eq(ConversationEntity::getProjectId, projectId)
                // deleted（逻辑删除）必须为正常。
                .eq(ConversationEntity::getDeleted, 0)
                // 按创建时间倒序排列。
                .orderByDesc(ConversationEntity::getCreateTime);
        // 执行查询并返回列表。
        return conversationMapper.selectList(queryWrapper);
    }

    @Override
    public void updateTitle(String conversationId, String title) {
        LambdaUpdateWrapper<ConversationEntity> updateWrapper = new LambdaUpdateWrapper<ConversationEntity>()
                .eq(ConversationEntity::getConversationId, conversationId)
                .eq(ConversationEntity::getDeleted, 0)
                .set(ConversationEntity::getTitle, title)
                .set(ConversationEntity::getUpdateTime, LocalDateTime.now());
        conversationMapper.update(null, updateWrapper);
    }

    @Override
    public void logicalDeleteByConversationId(String conversationId) {
        LambdaUpdateWrapper<ConversationEntity> updateWrapper = new LambdaUpdateWrapper<ConversationEntity>()
                .eq(ConversationEntity::getConversationId, conversationId)
                .eq(ConversationEntity::getDeleted, 0)
                .set(ConversationEntity::getDeleted, 1)
                .set(ConversationEntity::getUpdateTime, LocalDateTime.now());
        conversationMapper.update(null, updateWrapper);
    }

    /**
     * newId（生成编号）用于生成带业务前缀的随机编号。
     *
     * @param prefix 编号前缀
     * @return 随机编号
     */
    private String newId(String prefix) {
        // 使用 UUID（通用唯一识别码）生成本地开发阶段足够唯一的编号。
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
