package com.clipforge.knowledge.infrastructure.persistence.repository;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeBaseEntity;
import com.clipforge.knowledge.query.KnowledgeBaseQueryCondition;
import java.util.List;
import java.util.Optional;

/**
 * KnowledgeBaseRepository（知识库仓储）。
 *
 * <p>该接口负责保存和查询 KnowledgeBase（知识库），应用层只依赖该抽象，不直接依赖 MyBatis-Plus（MyBatis 增强工具）。
 */
public interface KnowledgeBaseRepository {

    /**
     * save（保存）用于写入知识库实体。
     *
     * @param entity 知识库实体
     * @return 已保存知识库实体
     */
    KnowledgeBaseEntity save(KnowledgeBaseEntity entity);

    /**
     * findByKnowledgeBaseId（按知识库编号查询）用于读取知识库详情。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 可能存在的知识库实体
     */
    Optional<KnowledgeBaseEntity> findByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * listByCondition（按条件查询列表）用于按用户、分类、平台、状态和启用状态筛选知识库。
     *
     * @param condition 查询条件
     * @return 知识库实体列表
     */
    List<KnowledgeBaseEntity> listByCondition(KnowledgeBaseQueryCondition condition);
}
