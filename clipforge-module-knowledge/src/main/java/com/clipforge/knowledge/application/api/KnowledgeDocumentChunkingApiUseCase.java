package com.clipforge.knowledge.application.api;

import com.clipforge.contract.knowledge.KnowledgeChunkResponse;
import com.clipforge.contract.knowledge.ProcessKnowledgeDocumentResponse;
import java.util.List;

/**
 * KnowledgeDocumentChunkingApiUseCase（知识文档切片接口用例）定义同步解析切片和切片列表查询能力。
 */
public interface KnowledgeDocumentChunkingApiUseCase {

    ProcessKnowledgeDocumentResponse processDocument(String documentId);

    List<KnowledgeChunkResponse> listChunks(String documentId);
}
