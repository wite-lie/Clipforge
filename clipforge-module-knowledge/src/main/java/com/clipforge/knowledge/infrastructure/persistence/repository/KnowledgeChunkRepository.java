package com.clipforge.knowledge.infrastructure.persistence.repository;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkMetadataEntity;
import java.util.List;
import java.util.Optional;

/**
 * KnowledgeChunkRepository（知识切片仓储）。
 *
 * <p>该接口负责保存、删除和查询 KnowledgeChunk（知识切片），应用层只依赖该抽象。
 */
public interface KnowledgeChunkRepository {

    /**
     * replaceByDocumentId（按文档编号替换切片）用于先删除旧切片，再批量写入新切片。
     *
     * @param documentId 知识文档编号
     * @param chunks 新切片列表
     * @param metadataList 新切片元数据列表
     */
    void replaceByDocumentId(String documentId, List<KnowledgeChunkEntity> chunks, List<KnowledgeChunkMetadataEntity> metadataList);

    /**
     * saveChunk（保存切片）用于人工新增单个切片。
     *
     * @param entity 切片实体
     * @return 已保存的切片实体
     */
    KnowledgeChunkEntity saveChunk(KnowledgeChunkEntity entity);

    /**
     * findByChunkId（按切片编号查询）用于读取单个切片详情。
     *
     * @param chunkId 知识切片编号
     * @return 可能存在的知识切片实体
     */
    Optional<KnowledgeChunkEntity> findByChunkId(String chunkId);

    /**
     * updateChunk（更新切片）用于保存人工修改后的切片内容。
     *
     * @param entity 切片实体
     * @return 更新后的切片实体
     */
    KnowledgeChunkEntity updateChunk(KnowledgeChunkEntity entity);

    /**
     * markEnabled（标记启用或禁用）用于切换切片是否参与检索。
     *
     * @param chunkId 知识切片编号
     * @param enabled 是否启用，1 表示启用，0 表示禁用
     * @return 更新后的切片实体
     */
    KnowledgeChunkEntity markEnabled(String chunkId, Integer enabled);

    /**
     * markDeleted（标记删除）用于逻辑删除单个切片。
     *
     * @param chunkId 知识切片编号
     * @return 更新后的切片实体
     */
    KnowledgeChunkEntity markDeleted(String chunkId);

    /**
     * markDeletedByDocumentId（按文档标记删除）用于批量删除某个文档下的全部切片。
     *
     * @param documentId 知识文档编号
     */
    void markDeletedByDocumentId(String documentId);

    /**
     * listByDocumentId（按文档编号查询切片）用于读取某个文档下的全部知识切片。
     *
     * @param documentId 知识文档编号
     * @return 知识切片实体列表
     */
    List<KnowledgeChunkEntity> listByDocumentId(String documentId);

    /**
     * listEnabledByKnowledgeBaseIds（按知识库编号查询启用切片）用于读取指定知识库下可检索的有效切片。
     *
     * @param knowledgeBaseIds 知识库编号列表
     * @return 启用且未删除的知识切片实体列表
     */
    List<KnowledgeChunkEntity> listEnabledByKnowledgeBaseIds(List<String> knowledgeBaseIds);

    /**
     * listAllEnabled（查询全部启用切片）用于全库检索兜底时读取全部可检索切片。
     *
     * @return 启用且未删除的知识切片实体列表
     */
    List<KnowledgeChunkEntity> listAllEnabled();
}
