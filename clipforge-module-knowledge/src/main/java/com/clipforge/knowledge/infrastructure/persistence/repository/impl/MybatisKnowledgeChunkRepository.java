package com.clipforge.knowledge.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkMetadataEntity;
import com.clipforge.knowledge.infrastructure.persistence.mapper.KnowledgeChunkMapper;
import com.clipforge.knowledge.infrastructure.persistence.mapper.KnowledgeChunkMetadataMapper;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeChunkRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * MybatisKnowledgeChunkRepository（MyBatis 知识切片仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问 cf_knowledge_chunk（知识切片表）。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisKnowledgeChunkRepository implements KnowledgeChunkRepository {

    /** knowledgeChunkMapper（知识切片映射器）负责执行知识切片表 CRUD（增删改查）。 */
    private final KnowledgeChunkMapper knowledgeChunkMapper;

    /** knowledgeChunkMetadataMapper（知识切片元数据映射器）负责执行切片元数据表 CRUD（增删改查）。 */
    private final KnowledgeChunkMetadataMapper knowledgeChunkMetadataMapper;

    /**
     * replaceByDocumentId（按文档编号替换切片）用于先逻辑删除旧切片，再批量写入新切片。
     *
     * @param documentId 知识文档编号
     * @param chunks 新切片列表
     * @param metadataList 新切片元数据列表
     */
    @Override
    @Transactional
    public void replaceByDocumentId(String documentId, List<KnowledgeChunkEntity> chunks, List<KnowledgeChunkMetadataEntity> metadataList) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 创建逻辑删除旧切片的更新条件。
        LambdaUpdateWrapper<KnowledgeChunkEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeChunkEntity>()
                // 设置 deleted（逻辑删除）为 1。
                .set(KnowledgeChunkEntity::getDeleted, 1)
                // 设置更新时间。
                .set(KnowledgeChunkEntity::getUpdateTime, now)
                // documentId（知识文档编号）必须匹配。
                .eq(KnowledgeChunkEntity::getDocumentId, documentId)
                // 只处理未删除数据。
                .eq(KnowledgeChunkEntity::getDeleted, 0);
        // 执行旧切片逻辑删除。
        knowledgeChunkMapper.update(null, updateWrapper);
        // 创建逻辑删除旧切片元数据的更新条件。
        LambdaUpdateWrapper<KnowledgeChunkMetadataEntity> metadataUpdateWrapper = new LambdaUpdateWrapper<KnowledgeChunkMetadataEntity>()
                // 设置 deleted（逻辑删除）为 1。
                .set(KnowledgeChunkMetadataEntity::getDeleted, 1)
                // 设置更新时间。
                .set(KnowledgeChunkMetadataEntity::getUpdateTime, now)
                // documentId（知识文档编号）必须匹配。
                .eq(KnowledgeChunkMetadataEntity::getDocumentId, documentId)
                // 只处理未删除数据。
                .eq(KnowledgeChunkMetadataEntity::getDeleted, 0);
        // 执行旧切片元数据逻辑删除。
        knowledgeChunkMetadataMapper.update(null, metadataUpdateWrapper);
        // 如果新切片为空，则无需插入。
        if (chunks == null || chunks.isEmpty()) {
            // 直接返回。
            return;
        }
        // 逐条插入新切片。
        for (KnowledgeChunkEntity chunk : chunks) {
            // 插入切片实体。
            knowledgeChunkMapper.insert(chunk);
        }
        // 如果新切片元数据为空，则无需插入。
        if (metadataList == null || metadataList.isEmpty()) {
            // 直接返回。
            return;
        }
        // 逐条插入新切片元数据。
        for (KnowledgeChunkMetadataEntity metadata : metadataList) {
            // 插入切片元数据实体。
            knowledgeChunkMetadataMapper.insert(metadata);
        }
    }

    @Override
    public KnowledgeChunkEntity saveChunk(KnowledgeChunkEntity entity) {
        entity.setUpdateTime(LocalDateTime.now());
        knowledgeChunkMapper.insert(entity);
        return entity;
    }

    /**
     * findByChunkId（按切片编号查询）用于读取单个切片详情。
     *
     * @param chunkId 知识切片编号
     * @return 可能存在的知识切片实体
     */
    @Override
    public java.util.Optional<KnowledgeChunkEntity> findByChunkId(String chunkId) {
        // 按 chunkId 查询未删除切片。
        LambdaQueryWrapper<KnowledgeChunkEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeChunkEntity>()
                .eq(KnowledgeChunkEntity::getChunkId, chunkId)
                .eq(KnowledgeChunkEntity::getDeleted, 0);
        // 返回查询结果。
        return java.util.Optional.ofNullable(knowledgeChunkMapper.selectOne(queryWrapper));
    }

    /**
     * updateChunk（更新切片）用于保存人工修改后的切片内容。
     *
     * @param entity 切片实体
     * @return 更新后的切片实体
     */
    @Override
    public KnowledgeChunkEntity updateChunk(KnowledgeChunkEntity entity) {
        // 刷新更新时间。
        entity.setUpdateTime(LocalDateTime.now());
        // 更新主表记录。
        knowledgeChunkMapper.updateById(entity);
        // 返回最新切片。
        return findByChunkId(entity.getChunkId()).orElseThrow();
    }

    /**
     * markEnabled（标记启用或禁用）用于切换切片是否参与检索。
     *
     * @param chunkId 知识切片编号
     * @param enabled 是否启用，1 表示启用，0 表示禁用
     * @return 更新后的切片实体
     */
    @Override
    public KnowledgeChunkEntity markEnabled(String chunkId, Integer enabled) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 构造状态更新条件。
        LambdaUpdateWrapper<KnowledgeChunkEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeChunkEntity>()
                .set(KnowledgeChunkEntity::getEnabled, enabled)
                .set(KnowledgeChunkEntity::getUpdateTime, now)
                .eq(KnowledgeChunkEntity::getChunkId, chunkId)
                .eq(KnowledgeChunkEntity::getDeleted, 0);
        // 执行更新。
        knowledgeChunkMapper.update(null, updateWrapper);
        // 返回更新后的切片。
        return findByChunkId(chunkId).orElseThrow();
    }

    /**
     * markDeleted（标记删除）用于逻辑删除单个切片。
     *
     * @param chunkId 知识切片编号
     * @return 更新后的切片实体
     */
    @Override
    public KnowledgeChunkEntity markDeleted(String chunkId) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 构造逻辑删除条件。
        LambdaUpdateWrapper<KnowledgeChunkEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeChunkEntity>()
                .set(KnowledgeChunkEntity::getDeleted, 1)
                .set(KnowledgeChunkEntity::getEnabled, 0)
                .set(KnowledgeChunkEntity::getUpdateTime, now)
                .eq(KnowledgeChunkEntity::getChunkId, chunkId)
                .eq(KnowledgeChunkEntity::getDeleted, 0);
        // 执行更新。
        knowledgeChunkMapper.update(null, updateWrapper);
        // 返回更新后的切片。
        return findByChunkId(chunkId).orElseThrow();
    }

    /**
     * markDeletedByDocumentId（按文档标记删除）用于批量删除某个文档下的全部切片。
     *
     * @param documentId 知识文档编号
     */
    @Override
    public void markDeletedByDocumentId(String documentId) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 构造按文档批量逻辑删除条件。
        LambdaUpdateWrapper<KnowledgeChunkEntity> updateWrapper = new LambdaUpdateWrapper<KnowledgeChunkEntity>()
                .set(KnowledgeChunkEntity::getDeleted, 1)
                .set(KnowledgeChunkEntity::getEnabled, 0)
                .set(KnowledgeChunkEntity::getUpdateTime, now)
                .eq(KnowledgeChunkEntity::getDocumentId, documentId)
                .eq(KnowledgeChunkEntity::getDeleted, 0);
        // 执行批量删除。
        knowledgeChunkMapper.update(null, updateWrapper);
    }

    /**
     * listByDocumentId（按文档编号查询切片）用于读取某个文档下的全部有效切片。
     *
     * @param documentId 知识文档编号
     * @return 知识切片实体列表
     */
    @Override
    public List<KnowledgeChunkEntity> listByDocumentId(String documentId) {
        // 创建按文档编号查询、排除逻辑删除并按切片序号升序排列的条件。
        LambdaQueryWrapper<KnowledgeChunkEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeChunkEntity>()
                // documentId（知识文档编号）必须匹配。
                .eq(KnowledgeChunkEntity::getDocumentId, documentId)
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeChunkEntity::getDeleted, 0)
                // 按切片序号升序排列，保持原文顺序。
                .orderByAsc(KnowledgeChunkEntity::getChunkIndex);
        // 执行列表查询并返回。
        return knowledgeChunkMapper.selectList(queryWrapper);
    }

    /**
     * listEnabledByKnowledgeBaseIds（按知识库编号查询启用切片）用于读取 BM25（最佳匹配 25）关键词检索候选切片。
     *
     * @param knowledgeBaseIds 知识库编号列表
     * @return 启用且未删除的知识切片实体列表
     */
    @Override
    public List<KnowledgeChunkEntity> listEnabledByKnowledgeBaseIds(List<String> knowledgeBaseIds) {
        // 如果知识库编号为空，则返回空列表。
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 创建按知识库编号查询、启用、未删除并保持原文顺序的条件。
        LambdaQueryWrapper<KnowledgeChunkEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeChunkEntity>()
                // knowledgeBaseId（知识库编号）必须在传入列表中。
                .in(KnowledgeChunkEntity::getKnowledgeBaseId, knowledgeBaseIds)
                // enabled（是否启用）必须为启用。
                .eq(KnowledgeChunkEntity::getEnabled, 1)
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeChunkEntity::getDeleted, 0)
                // 先按知识库编号排序，方便调试观察。
                .orderByAsc(KnowledgeChunkEntity::getKnowledgeBaseId)
                // 再按文档编号排序。
                .orderByAsc(KnowledgeChunkEntity::getDocumentId)
                // 最后按切片序号排序，保持原文顺序。
                .orderByAsc(KnowledgeChunkEntity::getChunkIndex);
        // 执行列表查询并返回。
        return knowledgeChunkMapper.selectList(queryWrapper);
    }

    /**
     * listAllEnabled（查询全部启用切片）用于全库检索兜底时读取 BM25（最佳匹配 25）候选切片。
     *
     * @return 启用且未删除的知识切片实体列表
     */
    @Override
    public List<KnowledgeChunkEntity> listAllEnabled() {
        // 创建不限制知识库编号、只读取启用且未删除切片的查询条件。
        LambdaQueryWrapper<KnowledgeChunkEntity> queryWrapper = new LambdaQueryWrapper<KnowledgeChunkEntity>()
                // enabled（是否启用）必须为启用。
                .eq(KnowledgeChunkEntity::getEnabled, 1)
                // deleted（逻辑删除）必须为正常。
                .eq(KnowledgeChunkEntity::getDeleted, 0)
                // 先按知识库编号排序，方便调试观察。
                .orderByAsc(KnowledgeChunkEntity::getKnowledgeBaseId)
                // 再按文档编号排序。
                .orderByAsc(KnowledgeChunkEntity::getDocumentId)
                // 最后按切片序号排序，保持原文顺序。
                .orderByAsc(KnowledgeChunkEntity::getChunkIndex);
        // 执行列表查询并返回。
        return knowledgeChunkMapper.selectList(queryWrapper);
    }
}
