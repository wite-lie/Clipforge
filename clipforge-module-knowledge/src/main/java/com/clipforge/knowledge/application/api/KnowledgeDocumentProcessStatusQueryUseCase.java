package com.clipforge.knowledge.application.api;

import com.clipforge.contract.knowledge.KnowledgeDocumentProcessStatusResponse;

/**
 * KnowledgeDocumentProcessStatusQueryUseCase（知识文档处理状态查询用例）定义状态查询入口。
 */
public interface KnowledgeDocumentProcessStatusQueryUseCase {

    KnowledgeDocumentProcessStatusResponse queryByDocumentId(String documentId);

    KnowledgeDocumentProcessStatusResponse queryByProcessLogId(String processLogId);
}
