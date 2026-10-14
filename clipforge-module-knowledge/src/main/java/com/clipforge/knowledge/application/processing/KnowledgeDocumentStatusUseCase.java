package com.clipforge.knowledge.application.processing;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;

/**
 * KnowledgeDocumentStatusUseCase（知识文档状态用例）。
 *
 * <p>该端口只负责 Knowledge 模块内部文档处理状态的写入动作，
 * 不暴露仓储实现，也不承载文档查询、消息发送或状态缓存职责。
 */
public interface KnowledgeDocumentStatusUseCase {

    /**
     * tryMarkProcessing（尝试标记处理中）用于以 CAS 方式抢占一次文档处理权。
     *
     * @param documentId 知识文档编号
     * @return true 表示抢占成功，false 表示文档不存在或已经处于处理中
     */
    boolean tryMarkProcessing(String documentId);

    /**
     * markChunked（标记已切片）用于解析和切片成功后更新文档状态。
     *
     * @param documentId 知识文档编号
     * @param chunkCount 切片数量
     * @param charCount 字符数
     * @param tokenCount 估算词元数量
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markChunked(String documentId, int chunkCount, int charCount, int tokenCount);

    /**
     * markEmbedded（标记已向量化）用于向量写入成功后更新文档状态。
     *
     * @param documentId 知识文档编号
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markEmbedded(String documentId);

    /**
     * markEmbeddingFailed（标记向量化失败）用于记录嵌入模型或向量写入失败。
     *
     * @param documentId 知识文档编号
     * @param errorMessage 失败原因
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markEmbeddingFailed(String documentId, String errorMessage);

    /**
     * markFailed（标记处理失败）用于记录解析或切片失败。
     *
     * @param documentId 知识文档编号
     * @param errorMessage 失败原因
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markFailed(String documentId, String errorMessage);

    /**
     * markReindexRequired（标记待重新索引）用于切片变更后使文档退出可检索状态。
     *
     * @param documentId 知识文档编号
     * @return 更新后的文档实体
     */
    KnowledgeDocumentEntity markReindexRequired(String documentId);
}
