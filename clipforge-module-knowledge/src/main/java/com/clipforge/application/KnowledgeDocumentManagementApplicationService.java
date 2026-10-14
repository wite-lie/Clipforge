package com.clipforge.application;

import com.clipforge.contract.knowledge.KnowledgeChunkResponse;
import com.clipforge.contract.knowledge.KnowledgeDocumentProcessLogResponse;
import com.clipforge.knowledge.application.api.KnowledgeDocumentManagementApiUseCase;
import com.clipforge.contract.knowledge.CreateKnowledgeChunkRequest;
import com.clipforge.contract.knowledge.KnowledgeDocumentResponse;
import com.clipforge.contract.knowledge.UpdateKnowledgeChunkRequest;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentProcessLogEntity;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentOverallStatus;
import com.clipforge.knowledge.application.document.KnowledgeDocumentManagementUseCase;
import com.clipforge.knowledge.application.chunk.KnowledgeChunkManagementUseCase;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentProcessLogRepository;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KnowledgeDocumentManagementApplicationService（知识文档管理应用服务）。
 *
 * <p>该服务负责文档与切片的管理接口：处理日志查询、切片新增/更新/启停/删除，以及文档启停/删除。
 */
@Service("knowledgeDocumentManagementApiApplicationService")
@RequiredArgsConstructor
public class KnowledgeDocumentManagementApplicationService implements KnowledgeDocumentManagementApiUseCase {

    /** knowledgeDocumentApplicationService（知识文档应用服务）用于复用文档详情响应转换。 */
    private final KnowledgeDocumentApplicationService knowledgeDocumentApplicationService;

    /** knowledgeDocumentRepositoryProvider（知识文档仓储提供器）用于读取和更新文档状态。 */
    private final ObjectProvider<KnowledgeDocumentRepository> knowledgeDocumentRepositoryProvider;

    /** knowledgeDocumentManagementUseCaseProvider（文档管理用例提供器）用于文档启停和文档删除。 */
    private final ObjectProvider<KnowledgeDocumentManagementUseCase> knowledgeDocumentManagementUseCaseProvider;

    /** knowledgeChunkManagementUseCaseProvider（知识切片管理用例提供器）用于执行切片业务操作。 */
    private final ObjectProvider<KnowledgeChunkManagementUseCase> knowledgeChunkManagementUseCaseProvider;

    /** processLogRepositoryProvider（处理日志仓储提供器）用于查询文档处理历史。 */
    private final ObjectProvider<KnowledgeDocumentProcessLogRepository> processLogRepositoryProvider;

    /**
     * listProcessLogs（查询处理日志）用于读取某个文档的全部处理历史。
     *
     * @param documentId 知识文档编号
     * @return 处理日志列表
     */
    public List<KnowledgeDocumentProcessLogResponse> listProcessLogs(String documentId) {
        requiredDocument(documentId);
        return requiredProcessLogRepository().listByDocumentId(documentId).stream()
                .map(this::toProcessLogResponse)
                .toList();
    }

    /**
     * queryChunk（查询单个切片）用于读取某个文档下的单个切片详情。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @return 知识切片响应
     */
    public KnowledgeChunkResponse queryChunk(String documentId, String chunkId) {
        return toChunkResponse(requiredKnowledgeChunkManagementUseCase()
                .findChunk(documentId, chunkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到当前文档下的知识切片，chunkId：" + chunkId)));
    }

    /**
     * createChunk（新增切片）用于人工补充一个切片。
     *
     * @param documentId 知识文档编号
     * @param request 新增切片请求
     * @return 新增后的切片响应
     */
    @Transactional
    public KnowledgeChunkResponse createChunk(String documentId, CreateKnowledgeChunkRequest request) {
        KnowledgeChunkManagementUseCase.CreateChunkCommand command = request == null ? null :
                new KnowledgeChunkManagementUseCase.CreateChunkCommand(
                        request.getContent(),
                        request.getTitlePath(),
                        request.getMetadataJson(),
                        request.getChunkIndex(),
                        request.getEnabled());
        return toChunkResponse(requiredKnowledgeChunkManagementUseCase().createChunk(documentId, command));
    }

    /**
     * updateChunk（更新切片）用于修改人工维护的切片内容。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @param request 更新切片请求
     * @return 更新后的切片响应
     */
    @Transactional
    public KnowledgeChunkResponse updateChunk(String documentId, String chunkId, UpdateKnowledgeChunkRequest request) {
        KnowledgeChunkManagementUseCase.UpdateChunkCommand command = request == null ? null :
                new KnowledgeChunkManagementUseCase.UpdateChunkCommand(
                        request.getTitlePath(),
                        request.getContent(),
                        request.getMetadataJson());
        return toChunkResponse(requiredKnowledgeChunkManagementUseCase().updateChunk(documentId, chunkId, command));
    }

    /**
     * setChunkEnabled（切片启停）用于临时让某个切片退出或重新参与检索。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @param enabled 是否启用
     * @return 更新后的切片响应
     */
    @Transactional
    public KnowledgeChunkResponse setChunkEnabled(String documentId, String chunkId, boolean enabled) {
        return toChunkResponse(requiredKnowledgeChunkManagementUseCase().setEnabled(documentId, chunkId, enabled));
    }

    /**
     * deleteChunk（删除切片）用于逻辑删除单个切片。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @return 被删除的切片编号
     */
    @Transactional
    public String deleteChunk(String documentId, String chunkId) {
        return requiredKnowledgeChunkManagementUseCase().deleteChunk(documentId, chunkId);
    }

    /**
     * setDocumentEnabled（文档启停）用于让文档临时下线或恢复。
     *
     * @param documentId 知识文档编号
     * @param enabled 是否启用
     * @return 文档响应
     */
    @Transactional
    public KnowledgeDocumentResponse setDocumentEnabled(String documentId, boolean enabled) {
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        assertDocumentNotProcessing(document);
        requiredKnowledgeDocumentManagementUseCase().setEnabled(documentId, enabled);
        return knowledgeDocumentApplicationService.queryDocument(documentId);
    }

    /**
     * deleteDocument（删除文档）用于逻辑删除文档与其全部切片。
     *
     * @param documentId 知识文档编号
     * @return 被删除的文档编号
     */
    @Transactional
    public String deleteDocument(String documentId) {
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        assertDocumentNotProcessing(document);
        return requiredKnowledgeDocumentManagementUseCase().deleteDocument(documentId);
    }

    /**
     * toProcessLogResponse（转换处理日志响应）用于对外返回可读的历史记录。
     *
     * @param entity 处理日志实体
     * @return 处理日志响应
     */
    private KnowledgeDocumentProcessLogResponse toProcessLogResponse(KnowledgeDocumentProcessLogEntity entity) {
        return KnowledgeDocumentProcessLogResponse.builder()
                .processLogId(entity.getProcessLogId())
                .documentId(entity.getDocumentId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .triggerType(entity.getTriggerType())
                .status(entity.getStatus())
                .phase(entity.getPhase())
                .parseDurationMs(entity.getParseDurationMs())
                .chunkDurationMs(entity.getChunkDurationMs())
                .embeddingDurationMs(entity.getEmbeddingDurationMs())
                .persistDurationMs(entity.getPersistDurationMs())
                .totalDurationMs(entity.getTotalDurationMs())
                .chunkCount(entity.getChunkCount())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .createTime(entity.getCreateTime())
                .build();
    }

    /**
     * toChunkResponse（转换切片响应）用于返回单个切片详情。
     *
     * @param entity 切片实体
     * @return 切片响应
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

    /**
     * requiredDocument（获取必需文档）用于校验文档存在。
     *
     * @param documentId 知识文档编号
     * @return 文档实体
     */
    private KnowledgeDocumentEntity requiredDocument(String documentId) {
        return requiredKnowledgeDocumentRepository().findByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档，documentId：" + documentId));
    }

    /**
     * requiredChunk（获取必需切片）用于校验切片存在。
     *
     * @param chunkId 知识切片编号
     * @return 切片实体
     */
    /**
     * assertDocumentNotProcessing（校验文档未处理中）用于避免与自动链路冲突。
     *
     * @param document 文档实体
     */
    private void assertDocumentNotProcessing(KnowledgeDocumentEntity document) {
        if (KnowledgeDocumentOverallStatus.PROCESSING.value().equals(document.getOverallStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文档正在处理中，暂不允许执行管理操作");
        }
    }

    /**
     * requiredKnowledgeDocumentRepository（获取必需文档仓储）。
     *
     * @return 文档仓储
     */
    private KnowledgeDocumentRepository requiredKnowledgeDocumentRepository() {
        KnowledgeDocumentRepository repository = knowledgeDocumentRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "文档管理接口需要启用 db Profile");
        }
        return repository;
    }

    /**
     * requiredKnowledgeDocumentManagementUseCase（获取必需文档管理用例）用于文档启停和删除。
     *
     * @return Knowledge 文档管理用例
     */
    private KnowledgeDocumentManagementUseCase requiredKnowledgeDocumentManagementUseCase() {
        // 从 Spring（Spring 框架）容器中获取文档管理用例。
        KnowledgeDocumentManagementUseCase useCase = knowledgeDocumentManagementUseCaseProvider.getIfAvailable();
        // 如果用例不存在，说明没有启用 db（数据库）Profile。
        if (useCase == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "文档管理需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用用例。
        return useCase;
    }

    /**
     * requiredKnowledgeChunkManagementUseCase（获取必需切片管理用例）用于执行切片管理操作。
     *
     * @return Knowledge 切片管理用例
     */
    private KnowledgeChunkManagementUseCase requiredKnowledgeChunkManagementUseCase() {
        // 从 Spring（Spring 框架）容器中获取切片管理用例。
        KnowledgeChunkManagementUseCase useCase = knowledgeChunkManagementUseCaseProvider.getIfAvailable();
        // 如果用例不存在，说明没有启用 db（数据库）Profile。
        if (useCase == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识切片管理需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用用例。
        return useCase;
    }

    /**
     * requiredProcessLogRepository（获取必需处理日志仓储）。
     *
     * @return 处理日志仓储
     */
    private KnowledgeDocumentProcessLogRepository requiredProcessLogRepository() {
        KnowledgeDocumentProcessLogRepository repository = processLogRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "处理日志查询需要启用 db Profile");
        }
        return repository;
    }

}
