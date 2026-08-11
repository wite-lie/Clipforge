package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentMetadataEntity;
import com.clipforge.infra.persistence.mapper.KnowledgeDocumentMetadataMapper;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentMetadataRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisKnowledgeDocumentMetadataRepository（MyBatis 知识文档元数据仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisKnowledgeDocumentMetadataRepository implements KnowledgeDocumentMetadataRepository {

    /** metadataMapper（元数据映射器）负责执行文档元数据表 CRUD（增删改查）。 */
    private final KnowledgeDocumentMetadataMapper metadataMapper;

    /**
     * saveAll（批量保存）用于写入文档元数据。
     *
     * @param metadataList 文档元数据列表
     */
    @Override
    public void saveAll(List<KnowledgeDocumentMetadataEntity> metadataList) {
        // 如果元数据列表为空，则无需写入。
        if (metadataList == null || metadataList.isEmpty()) {
            // 直接返回。
            return;
        }
        // 逐条插入元数据。
        for (KnowledgeDocumentMetadataEntity metadata : metadataList) {
            // 插入元数据实体。
            metadataMapper.insert(metadata);
        }
    }

    /**
     * upsertByDocumentIdAndKey（按文档编号和元数据键保存或更新）用于避免重复键冲突。
     *
     * @param metadata 文档元数据实体
     */
    @Override
    public void upsertByDocumentIdAndKey(KnowledgeDocumentMetadataEntity metadata) {
        // 如果元数据为空，则直接返回。
        if (metadata == null) {
            // 结束处理。
            return;
        }
        // 先尝试按文档编号和元数据键查找已有记录。
        KnowledgeDocumentMetadataEntity existing = metadataMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentMetadataEntity>()
                // 文档编号必须匹配。
                .eq(KnowledgeDocumentMetadataEntity::getDocumentId, metadata.getDocumentId())
                // 元数据键必须匹配。
                .eq(KnowledgeDocumentMetadataEntity::getMetadataKey, metadata.getMetadataKey())
                // 只更新未删除记录。
                .eq(KnowledgeDocumentMetadataEntity::getDeleted, 0)
                // 限制只查一条。
                .last("limit 1"));
        // 如果已存在，则更新值和时间。
        if (existing != null) {
            // 执行更新。
            metadataMapper.update(null, new LambdaUpdateWrapper<KnowledgeDocumentMetadataEntity>()
                    // 设置元数据值。
                    .set(KnowledgeDocumentMetadataEntity::getMetadataValue, metadata.getMetadataValue())
                    // 设置值类型。
                    .set(KnowledgeDocumentMetadataEntity::getValueType, metadata.getValueType())
                    // 设置来源类型。
                    .set(KnowledgeDocumentMetadataEntity::getSourceType, metadata.getSourceType())
                    // 设置置信度。
                    .set(KnowledgeDocumentMetadataEntity::getConfidence, metadata.getConfidence())
                    // 设置启用状态。
                    .set(KnowledgeDocumentMetadataEntity::getEnabled, metadata.getEnabled())
                    // 设置更新时间。
                    .set(KnowledgeDocumentMetadataEntity::getUpdateTime, metadata.getUpdateTime())
                    // 文档编号匹配。
                    .eq(KnowledgeDocumentMetadataEntity::getDocumentId, metadata.getDocumentId())
                    // 元数据键匹配。
                    .eq(KnowledgeDocumentMetadataEntity::getMetadataKey, metadata.getMetadataKey())
                    // 只更新未删除记录。
                    .eq(KnowledgeDocumentMetadataEntity::getDeleted, 0));
            // 返回。
            return;
        }
        // 不存在则直接插入。
        metadataMapper.insert(metadata);
    }
}
