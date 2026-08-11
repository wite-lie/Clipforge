package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.infra.persistence.entity.ConversationMessageEntity;
import com.clipforge.infra.persistence.mapper.ConversationMessageMapper;
import com.clipforge.infra.persistence.repository.ConversationMessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisConversationMessageRepository（MyBatis 会话消息仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问会话消息表。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisConversationMessageRepository implements ConversationMessageRepository {

    /** conversationMessageMapper（会话消息映射器）负责执行会话消息表 CRUD（增删改查）。 */
    private final ConversationMessageMapper conversationMessageMapper;

    /**
     * append（追加）用于向会话中追加一条消息。
     *
     * @param conversationId 会话编号
     * @param runId 运行编号
     * @param role 消息角色
     * @param content 消息内容
     * @return 已保存消息实体
     */
    @Override
    public ConversationMessageEntity append(String conversationId, String runId, String role, String content) {
        // 使用 Builder（建造者模式）组装会话消息实体。
        ConversationMessageEntity entity = ConversationMessageEntity.builder()
                // 设置数据库内部主键。
                .id(newId("message_pk"))
                // 设置对外消息编号。
                .messageId(newId("message"))
                // 设置会话编号。
                .conversationId(conversationId)
                // 设置运行编号。
                .runId(runId)
                // 设置消息角色。
                .role(role)
                // 设置消息内容。
                .content(content)
                // 设置创建时间。
                .createTime(LocalDateTime.now())
                // 设置逻辑删除标记为正常。
                .deleted(0)
                // 构建实体。
                .build();
        // 插入数据库。
        conversationMessageMapper.insert(entity);
        // 返回已保存消息实体。
        return entity;
    }

    /**
     * listByConversationId（按会话编号查询列表）用于读取会话消息。
     *
     * @param conversationId 会话编号
     * @return 会话消息实体列表
     */
    @Override
    public List<ConversationMessageEntity> listByConversationId(String conversationId) {
        // 创建按会话编号查询、排除逻辑删除并按创建时间升序排列的条件。
        LambdaQueryWrapper<ConversationMessageEntity> queryWrapper = new LambdaQueryWrapper<ConversationMessageEntity>()
                // conversationId（会话编号）必须匹配。
                .eq(ConversationMessageEntity::getConversationId, conversationId)
                // deleted（逻辑删除）必须为正常。
                .eq(ConversationMessageEntity::getDeleted, 0)
                // 按创建时间升序排列，便于还原多轮对话顺序。
                .orderByAsc(ConversationMessageEntity::getCreateTime);
        // 执行查询并返回列表。
        return conversationMessageMapper.selectList(queryWrapper);
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
