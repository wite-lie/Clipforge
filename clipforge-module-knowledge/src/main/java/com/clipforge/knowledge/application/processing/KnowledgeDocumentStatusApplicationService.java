package com.clipforge.knowledge.application.processing;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * KnowledgeDocumentStatusApplicationService（知识文档状态应用服务）。
 *
 * <p>该服务是 Knowledge 模块文档状态写入的统一入口。状态具体如何落库仍由仓储实现负责，
 * Redis（缓存）、RocketMQ（消息队列）和 API（接口）层不进入本服务。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class KnowledgeDocumentStatusApplicationService implements KnowledgeDocumentStatusUseCase {

    /** knowledgeDocumentRepository（知识文档仓储）负责执行状态更新。 */
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    /**
     * tryMarkProcessing（尝试标记处理中）用于以仓储提供的 CAS 更新抢占处理权。
     *
     * @param documentId 知识文档编号
     * @return 是否抢占成功
     */
    @Override
    public boolean tryMarkProcessing(String documentId) {
        return knowledgeDocumentRepository.markProcessingIfNotProcessing(documentId);
    }

    /**
     * markChunked（标记已切片）用于转发切片成功状态更新。
     *
     * @param documentId 知识文档编号
     * @param chunkCount 切片数量
     * @param charCount 字符数
     * @param tokenCount 估算词元数量
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markChunked(String documentId, int chunkCount, int charCount, int tokenCount) {
        return knowledgeDocumentRepository.markChunked(documentId, chunkCount, charCount, tokenCount);
    }

    /**
     * markEmbedded（标记已向量化）用于转发向量化成功状态更新。
     *
     * @param documentId 知识文档编号
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markEmbedded(String documentId) {
        return knowledgeDocumentRepository.markEmbedded(documentId);
    }

    /**
     * markEmbeddingFailed（标记向量化失败）用于转发向量化失败状态更新。
     *
     * @param documentId 知识文档编号
     * @param errorMessage 失败原因
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markEmbeddingFailed(String documentId, String errorMessage) {
        return knowledgeDocumentRepository.markEmbeddingFailed(documentId, errorMessage);
    }

    /**
     * markFailed（标记处理失败）用于转发解析或切片失败状态更新。
     *
     * @param documentId 知识文档编号
     * @param errorMessage 失败原因
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markFailed(String documentId, String errorMessage) {
        return knowledgeDocumentRepository.markFailed(documentId, errorMessage);
    }

    /**
     * markReindexRequired（标记待重新索引）用于转发切片变更后的状态更新。
     *
     * @param documentId 知识文档编号
     * @return 更新后的文档实体
     */
    @Override
    public KnowledgeDocumentEntity markReindexRequired(String documentId) {
        return knowledgeDocumentRepository.markReindexRequired(documentId);
    }
}
