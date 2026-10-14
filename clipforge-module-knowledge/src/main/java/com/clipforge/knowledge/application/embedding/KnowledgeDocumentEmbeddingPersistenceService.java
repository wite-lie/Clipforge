package com.clipforge.knowledge.application.embedding;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.application.processing.KnowledgeDocumentStatusUseCase;
import com.clipforge.knowledge.vector.VectorStore;
import com.clipforge.knowledge.vector.VectorStoreReplaceDocumentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KnowledgeDocumentEmbeddingPersistenceService（知识文档向量化持久化服务）。
 *
 * <p>该服务只在 db（数据库）Profile（配置环境）下启用，用于把“向量表替换”和“文档状态更新”放进同一个短事务。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class KnowledgeDocumentEmbeddingPersistenceService {

    /** vectorStore（向量存储）负责写入 cf_knowledge_embedding（知识向量表）。 */
    private final VectorStore vectorStore;

    /** knowledgeDocumentStatusUseCase（知识文档状态用例）负责更新文档向量化状态。 */
    private final KnowledgeDocumentStatusUseCase knowledgeDocumentStatusUseCase;

    /**
     * replaceEmbeddingsAndMarkReady（替换向量并标记可检索）用于在同一个短事务中完成向量替换和状态更新。
     *
     * @param request 向量替换请求
     * @return 更新后的知识文档实体
     */
    @Transactional
    public KnowledgeDocumentEntity replaceEmbeddingsAndMarkReady(VectorStoreReplaceDocumentRequest request) {
        // 替换该文档旧向量并写入新向量。
        vectorStore.replaceByDocumentId(request);
        // 向量写入成功后更新文档状态为 READY（可检索）。
        return knowledgeDocumentStatusUseCase.markEmbedded(request.getDocumentId());
    }
}
