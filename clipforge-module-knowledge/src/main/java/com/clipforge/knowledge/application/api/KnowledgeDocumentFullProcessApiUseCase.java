package com.clipforge.knowledge.application.api;

import com.clipforge.contract.knowledge.FullProcessKnowledgeDocumentResponse;

/**
 * KnowledgeDocumentFullProcessApiUseCase（知识文档完整处理接口用例）定义同步完整处理入口。
 */
public interface KnowledgeDocumentFullProcessApiUseCase {

    FullProcessKnowledgeDocumentResponse fullProcessDocument(String documentId);
}
