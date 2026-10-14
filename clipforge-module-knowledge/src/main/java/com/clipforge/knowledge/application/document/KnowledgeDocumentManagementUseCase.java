package com.clipforge.knowledge.application.document;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import java.util.List;
import java.util.Optional;

/**
 * KnowledgeDocumentManagementUseCase（知识文档管理用例）。
 *
 * <p>该端口提供 Knowledge 模块内的文档查询、启停和逻辑删除能力，
 * 不暴露 API DTO、文件上传实现或 URL 同步基础设施。
 */
public interface KnowledgeDocumentManagementUseCase {

    /**
     * findByDocumentId（按文档编号查询）用于读取未逻辑删除的知识文档。
     *
     * @param documentId 知识文档编号
     * @return 可能存在的知识文档
     */
    Optional<KnowledgeDocumentEntity> findByDocumentId(String documentId);

    /**
     * listByKnowledgeBaseId（按知识库编号查询）用于读取知识库下的文档列表。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 文档列表
     */
    List<KnowledgeDocumentEntity> listByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * setEnabled（设置启用状态）用于切换文档是否参与检索。
     *
     * @param documentId 知识文档编号
     * @param enabled 是否启用
     * @return 更新后的文档
     */
    KnowledgeDocumentEntity setEnabled(String documentId, boolean enabled);

    /**
     * deleteDocument（删除文档）用于在同一事务中逻辑删除文档和全部切片。
     *
     * @param documentId 知识文档编号
     * @return 被删除的文档编号
     */
    String deleteDocument(String documentId);
}
