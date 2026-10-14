package com.clipforge.knowledge.application.api;

import com.clipforge.contract.knowledge.CreateKnowledgeChunkRequest;
import com.clipforge.contract.knowledge.KnowledgeChunkResponse;
import com.clipforge.contract.knowledge.KnowledgeDocumentProcessLogResponse;
import com.clipforge.contract.knowledge.UpdateKnowledgeChunkRequest;
import com.clipforge.contract.knowledge.KnowledgeDocumentResponse;
import java.util.List;

/**
 * KnowledgeDocumentManagementApiUseCase（知识文档管理接口用例）定义处理日志、切片和文档管理入口。
 */
public interface KnowledgeDocumentManagementApiUseCase {

    List<KnowledgeDocumentProcessLogResponse> listProcessLogs(String documentId);

    KnowledgeChunkResponse queryChunk(String documentId, String chunkId);

    KnowledgeChunkResponse createChunk(String documentId, CreateKnowledgeChunkRequest request);

    KnowledgeChunkResponse updateChunk(String documentId, String chunkId, UpdateKnowledgeChunkRequest request);

    KnowledgeChunkResponse setChunkEnabled(String documentId, String chunkId, boolean enabled);

    String deleteChunk(String documentId, String chunkId);

    KnowledgeDocumentResponse setDocumentEnabled(String documentId, boolean enabled);

    String deleteDocument(String documentId);
}
