package com.clipforge.application;

import com.clipforge.contract.knowledge.KnowledgeChunkResponse;
import com.clipforge.contract.knowledge.ProcessKnowledgeDocumentResponse;
import com.clipforge.knowledge.application.api.KnowledgeDocumentChunkingApiUseCase;
import com.clipforge.contract.knowledge.KnowledgeDocumentResponse;
import com.clipforge.knowledge.application.processing.KnowledgeDocumentChunkingResult;
import com.clipforge.knowledge.application.processing.KnowledgeDocumentChunkingUseCase;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * KnowledgeDocumentProcessApplicationService（知识文档处理 API 适配服务）。
 *
 * <p>解析、切片、元数据同步和文档状态更新已经由 Knowledge 模块负责，
 * 本类只把 Knowledge 内部结果转换为现有 API DTO，保持 Controller 契约不变。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentProcessApplicationService implements KnowledgeDocumentChunkingApiUseCase {

    /** chunkingUseCase（知识文档解析切片用例）负责实际 Knowledge 领域处理。 */
    private final KnowledgeDocumentChunkingUseCase chunkingUseCase;

    /**
     * processDocument（处理文档）用于保持原有同步接口行为。
     *
     * @param documentId 知识文档编号
     * @return API 处理响应
     */
    public ProcessKnowledgeDocumentResponse processDocument(String documentId) {
        KnowledgeDocumentChunkingResult result = chunkingUseCase.processDocument(documentId);
        return ProcessKnowledgeDocumentResponse.builder()
                .document(toDocumentResponse(result.getDocument()))
                .chunksPreview(result.getChunksPreview().stream().map(this::toChunkResponse).toList())
                .build();
    }

    /**
     * listChunks（查询切片）用于保持原有切片查询接口行为。
     *
     * @param documentId 知识文档编号
     * @return API 切片响应列表
     */
    public List<KnowledgeChunkResponse> listChunks(String documentId) {
        return chunkingUseCase.listChunks(documentId).stream()
                .map(this::toChunkResponse)
                .toList();
    }

    /**
     * toDocumentResponse（转换文档响应）用于隔离 API 契约和 Knowledge 实体。
     *
     * @param entity Knowledge 文档实体
     * @return API 文档响应
     */
    private KnowledgeDocumentResponse toDocumentResponse(KnowledgeDocumentEntity entity) {
        return KnowledgeDocumentResponse.builder()
                .documentId(entity.getDocumentId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .userId(entity.getUserId())
                .sourceType(entity.getSourceType())
                .sourceLocation(entity.getSourceLocation())
                .originalFilename(entity.getOriginalFilename())
                .storedFilename(entity.getStoredFilename())
                .filePath(entity.getFilePath())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .fileHash(entity.getFileHash())
                .parseStatus(entity.getParseStatus())
                .chunkStatus(entity.getChunkStatus())
                .embeddingStatus(entity.getEmbeddingStatus())
                .overallStatus(entity.getOverallStatus())
                .errorMessage(entity.getErrorMessage())
                .chunkStrategy(entity.getChunkStrategy())
                .chunkConfigJson(entity.getChunkConfigJson())
                .metadataJson(entity.getMetadataJson())
                .chunkCount(entity.getChunkCount())
                .charCount(entity.getCharCount())
                .tokenCount(entity.getTokenCount())
                .enabled(entity.getEnabled())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    /**
     * toChunkResponse（转换切片响应）用于隔离 API 契约和 Knowledge 实体。
     *
     * @param entity Knowledge 切片实体
     * @return API 切片响应
     */
    private KnowledgeChunkResponse toChunkResponse(KnowledgeChunkEntity entity) {
        return KnowledgeChunkResponse.builder()
                .chunkId(entity.getChunkId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .documentId(entity.getDocumentId())
                .chunkIndex(entity.getChunkIndex())
                .titlePath(entity.getTitlePath())
                .content(entity.getContent())
                .contentHash(entity.getContentHash())
                .charCount(entity.getCharCount())
                .tokenCount(entity.getTokenCount())
                .metadataJson(entity.getMetadataJson())
                .enabled(entity.getEnabled())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
