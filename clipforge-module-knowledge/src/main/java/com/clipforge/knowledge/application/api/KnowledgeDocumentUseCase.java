package com.clipforge.knowledge.application.api;

import com.clipforge.contract.knowledge.CreateKnowledgeUrlDocumentRequest;
import com.clipforge.contract.knowledge.KnowledgeDocumentResponse;
import java.util.List;

/**
 * KnowledgeDocumentUseCase（知识文档接口用例）定义文档上传、URL 登记和文档查询能力。
 *
 * <p>该接口属于 Knowledge 模块的应用边界，不依赖 Spring Web、legacy 编排实现或消息队列细节。
 */
public interface KnowledgeDocumentUseCase {

    KnowledgeDocumentResponse uploadDocument(KnowledgeDocumentUploadCommand command);

    KnowledgeDocumentResponse createUrlDocument(String knowledgeBaseId, String sourceUrl, String userId);

    default KnowledgeDocumentResponse createUrlDocument(String knowledgeBaseId, CreateKnowledgeUrlDocumentRequest request) {
        return createUrlDocument(
                knowledgeBaseId,
                request == null ? null : request.getSourceUrl(),
                request == null ? null : request.getUserId());
    }

    List<KnowledgeDocumentResponse> listDocuments(String knowledgeBaseId);

    KnowledgeDocumentResponse queryDocument(String documentId);
}
