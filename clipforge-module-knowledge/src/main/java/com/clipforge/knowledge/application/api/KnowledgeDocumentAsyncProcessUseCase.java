package com.clipforge.knowledge.application.api;

import com.clipforge.contract.knowledge.AsyncProcessKnowledgeDocumentResponse;

/**
 * KnowledgeDocumentAsyncProcessUseCase（知识文档异步处理接口用例）定义 MQ 异步处理提交入口。
 */
public interface KnowledgeDocumentAsyncProcessUseCase {

    AsyncProcessKnowledgeDocumentResponse startAsyncProcess(String documentId);
}
