package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.KnowledgeDocumentEntity;
import java.util.List;
import java.util.Optional;

/**
 * KnowledgeDocumentRepository（知识文档仓储）。
 *
 * <p>该接口负责保存和查询 KnowledgeDocument（知识文档），应用层只依赖该抽象。
 */
public interface KnowledgeDocumentRepository {

    /**
     * save（保存）用于写入知识文档实体。
     *
     * @param entity 知识文档实体
     * @return 已保存知识文档实体
     */
    KnowledgeDocumentEntity save(KnowledgeDocumentEntity entity);

    /**
     * findByDocumentId（按知识文档编号查询）用于读取文档详情。
     *
     * @param documentId 知识文档编号
     * @return 可能存在的知识文档实体
     */
    Optional<KnowledgeDocumentEntity> findByDocumentId(String documentId);

    /**
     * listByKnowledgeBaseId（按知识库编号查询列表）用于读取某个知识库下的文档。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 知识文档实体列表
     */
    List<KnowledgeDocumentEntity> listByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * markEnabled（标记启用或禁用）用于切换文档是否参与检索。
     *
     * @param documentId 知识文档编号
     * @param enabled 是否启用，1 表示启用，0 表示禁用
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markEnabled(String documentId, Integer enabled);

    /**
     * markDeleted（标记删除）用于将文档逻辑删除。
     *
     * @param documentId 知识文档编号
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markDeleted(String documentId);

    /**
     * markProcessingIfNotProcessing（非处理中时标记处理中）用于异步任务提交前做轻量 CAS（比较并交换）防重复处理。
     *
     * @param documentId 知识文档编号
     * @return true 表示成功标记为处理中，false 表示文档不存在或已经处于处理中
     */
    boolean markProcessingIfNotProcessing(String documentId);

    /**
     * markChunked（标记已切片）用于在文档解析和切片成功后更新文档状态。
     *
     * @param documentId 知识文档编号
     * @param chunkCount 切片数量
     * @param charCount 字符数
     * @param tokenCount 估算 Token（词元）数量
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markChunked(String documentId, int chunkCount, int charCount, int tokenCount);

    /**
     * markEmbedded（标记已向量化）用于在文档全部有效切片向量写入成功后更新文档状态。
     *
     * @param documentId 知识文档编号
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markEmbedded(String documentId);

    /**
     * markEmbeddingFailed（标记向量化失败）用于在 Embedding（嵌入）或向量入库失败后更新文档状态。
     *
     * @param documentId 知识文档编号
     * @param errorMessage 错误消息
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markEmbeddingFailed(String documentId, String errorMessage);

    /**
     * markReindexRequired（标记待重新索引）用于在切片被人工修改后让文档退出可检索状态。
     *
     * @param documentId 知识文档编号
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markReindexRequired(String documentId);

    /**
     * refreshRemoteSourceDocument（刷新远程来源文档）用于在 URL（统一资源定位符）内容变化后更新本地文档快照。
     *
     * @param documentId 知识文档编号
     * @param sourceLocation 原始 URL 地址
     * @param originalFilename 原始文件名
     * @param storedFilename 系统存储文件名
     * @param filePath 本地文件路径
     * @param contentType 内容类型
     * @param fileSize 文件大小
     * @param fileHash 文件哈希
     * @param lastEtag 最近 ETag
     * @param lastModified 最近修改时间
     * @param lastContentHash 最近内容哈希
     * @return 更新后的知识文档实体
     */
    KnowledgeDocumentEntity refreshRemoteSourceDocument(String documentId, String sourceLocation, String originalFilename, String storedFilename, String filePath, String contentType, Long fileSize, String fileHash, String lastEtag, String lastModified, String lastContentHash);

    /**
     * updateScheduleConfig（更新定时同步配置）用于同步文档表里的定时字段。
     *
     * @param documentId 知识文档编号
     * @param scheduleEnabled 是否启用定时同步
     * @param scheduleCron Cron（定时表达式）
     * @return 更新后的知识文档实体
     */
    KnowledgeDocumentEntity updateScheduleConfig(String documentId, Integer scheduleEnabled, String scheduleCron);

    /**
     * markFailed（标记失败）用于在文档解析或切片失败后更新文档状态。
     *
     * @param documentId 知识文档编号
     * @param errorMessage 错误消息
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markFailed(String documentId, String errorMessage);
}
