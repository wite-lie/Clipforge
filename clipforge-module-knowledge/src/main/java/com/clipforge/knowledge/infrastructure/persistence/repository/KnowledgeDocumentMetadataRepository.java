package com.clipforge.knowledge.infrastructure.persistence.repository;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentMetadataEntity;
import java.util.List;

/**
 * KnowledgeDocumentMetadataRepository（知识文档元数据仓储）。
 *
 * <p>该接口负责批量保存文档级键值元数据。
 */
public interface KnowledgeDocumentMetadataRepository {

    /**
     * saveAll（批量保存）用于写入文档元数据。
     *
     * @param metadataList 文档元数据列表
     */
    void saveAll(List<KnowledgeDocumentMetadataEntity> metadataList);

    /**
     * upsertByDocumentIdAndKey（按文档编号和元数据键保存或更新）用于避免同一文档重复插入相同键值。
     *
     * @param metadata 文档元数据实体
     */
    void upsertByDocumentIdAndKey(KnowledgeDocumentMetadataEntity metadata);
}
