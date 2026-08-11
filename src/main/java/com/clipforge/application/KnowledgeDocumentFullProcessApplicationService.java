package com.clipforge.application;

import com.clipforge.api.dto.FullProcessKnowledgeDocumentResponse;
import com.clipforge.api.dto.KnowledgeDocumentResponse;
import com.clipforge.api.dto.ProcessKnowledgeDocumentResponse;
import com.clipforge.application.KnowledgeDocumentEmbeddingApplicationService.KnowledgeDocumentEmbeddingResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentProcessLogEntity;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentProcessLogStatus;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentProcessPhase;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentProcessTriggerType;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentProcessLogRepository;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentRepository;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatus;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatusCache;
import com.clipforge.rag.recovery.KnowledgeDocumentRetryStage;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * KnowledgeDocumentFullProcessApplicationService（知识文档完整处理应用服务）。
 *
 * <p>该服务负责把 Phase 7.4（文档解析与切片）和 Phase 7.6（Embedding 嵌入与 pgvector 入库）串成一个同步端到端流程。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentFullProcessApplicationService {

    /** knowledgeDocumentRepositoryProvider（知识文档仓储提供器）用于读取文档归属和最终状态。 */
    private final ObjectProvider<KnowledgeDocumentRepository> knowledgeDocumentRepositoryProvider;

    /** processLogRepositoryProvider（处理日志仓储提供器）用于记录完整处理链路。 */
    private final ObjectProvider<KnowledgeDocumentProcessLogRepository> processLogRepositoryProvider;

    /** statusCacheProvider（状态缓存提供器）用于在 Redis（缓存）中刷新异步处理进度。 */
    private final ObjectProvider<KnowledgeDocumentProcessStatusCache> statusCacheProvider;

    /** processApplicationService（文档处理应用服务）负责解析和切片。 */
    private final KnowledgeDocumentProcessApplicationService processApplicationService;

    /** embeddingApplicationService（文档向量化应用服务）负责 Embedding（嵌入）和 pgvector（PostgreSQL 向量扩展）入库。 */
    private final KnowledgeDocumentEmbeddingApplicationService embeddingApplicationService;

    /**
     * fullProcessDocument（完整处理文档）用于同步执行解析、切片、向量化和处理日志记录。
     *
     * @param documentId 知识文档编号
     * @return 完整处理响应
     */
    public FullProcessKnowledgeDocumentResponse fullProcessDocument(String documentId) {
        // 查询目标文档，确保后续日志能记录知识库编号。
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        // 创建处理日志，后续成功或失败都会更新同一条日志。
        KnowledgeDocumentProcessLogEntity processLog = createRunningProcessLog(document, KnowledgeDocumentProcessTriggerType.MANUAL);
        // 执行完整处理流程。
        return doFullProcessDocument(documentId, processLog);
    }

    /**
     * fullProcessDocumentWithExistingLog（使用已有日志完整处理文档）用于 RocketMQ（消息队列）消费者复用同步完整处理链路。
     *
     * @param documentId 知识文档编号
     * @param processLogId 已经创建好的处理日志编号
     * @return 完整处理响应
     */
    public FullProcessKnowledgeDocumentResponse fullProcessDocumentWithExistingLog(String documentId, String processLogId) {
        // 默认从完整处理入口恢复，保持历史调用行为不变。
        return fullProcessDocumentWithExistingLogAndRetryStage(documentId, processLogId, KnowledgeDocumentRetryStage.FULL_PROCESS);
    }

    /**
     * fullProcessDocumentWithExistingLogAndRetryStage（按重试阶段使用已有日志处理文档）用于恢复任务选择更精确的处理入口。
     *
     * @param documentId 知识文档编号
     * @param processLogId 已经创建好的处理日志编号
     * @param retryStage 重试阶段
     * @return 完整处理响应
     */
    public FullProcessKnowledgeDocumentResponse fullProcessDocumentWithExistingLogAndRetryStage(String documentId, String processLogId, KnowledgeDocumentRetryStage retryStage) {
        // 查询已有处理日志，避免异步链路重复创建处理日志。
        KnowledgeDocumentProcessLogEntity processLog = requiredProcessLogRepository().findByProcessLogId(processLogId)
                // 处理日志不存在时抛出明确错误。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档处理日志，processLogId：" + processLogId));
        // 校验消息中的文档编号和日志中的文档编号一致，避免误处理其他文档。
        if (!documentId.equals(processLog.getDocumentId())) {
            // 抛出请求错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "处理日志和知识文档不匹配，documentId：" + documentId + "，processLogId：" + processLogId);
        }
        // 如果日志已经成功，说明可能发生了 MQ（消息队列）重复投递，直接返回当前文档状态，避免重复消耗 Embedding（嵌入）模型成本。
        if (KnowledgeDocumentProcessLogStatus.SUCCESS.value().equals(processLog.getStatus())) {
            // 查询最新文档状态。
            KnowledgeDocumentEntity document = requiredDocument(documentId);
            // 返回幂等成功响应。
            return FullProcessKnowledgeDocumentResponse.builder()
                    // 设置处理日志编号。
                    .processLogId(processLogId)
                    // 设置文档响应。
                    .document(toDocumentResponse(document))
                    // 重复消费时没有重新执行解析切片响应。
                    .processResponse(null)
                    // 设置切片数量。
                    .chunkCount(processLog.getChunkCount())
                    // 重复消费时不重新统计向量数量。
                    .embeddingCount(null)
                    // 设置耗时。
                    .chunkDurationMs(processLog.getChunkDurationMs())
                    // 设置向量化耗时。
                    .embeddingDurationMs(processLog.getEmbeddingDurationMs())
                    // 设置总耗时。
                    .totalDurationMs(processLog.getTotalDurationMs())
                    // 设置处理状态。
                    .status(KnowledgeDocumentProcessLogStatus.SUCCESS.value())
                    // 构建响应。
                    .build();
        }
        // 如果重试阶段要求从向量化或向量入库恢复，则跳过解析切片，直接复用已有切片重新向量化。
        if (retryStage == KnowledgeDocumentRetryStage.EMBEDDING || retryStage == KnowledgeDocumentRetryStage.PERSIST_VECTOR) {
            // 执行仅向量化恢复流程。
            return doEmbeddingOnlyProcessDocument(documentId, processLog);
        }
        // 其他阶段复用同一条处理日志执行完整处理流程。
        return doFullProcessDocument(documentId, processLog);
    }

    /**
     * doFullProcessDocument（执行完整处理文档）用于复用同步接口和异步消费者的核心处理逻辑。
     *
     * @param documentId 知识文档编号
     * @param processLog 处理日志实体
     * @return 完整处理响应
     */
    private FullProcessKnowledgeDocumentResponse doFullProcessDocument(String documentId, KnowledgeDocumentProcessLogEntity processLog) {
        // 记录完整链路开始时间，用于计算总耗时。
        long totalStart = System.currentTimeMillis();
        // 初始化阶段耗时，失败时也能写入已经完成的部分。
        Long chunkDurationMs = null;
        // 初始化向量化耗时，失败时也能写入已经完成的部分。
        Long embeddingDurationMs = null;
        // 记录当前阶段，失败时用于定位问题。
        KnowledgeDocumentProcessPhase currentPhase = KnowledgeDocumentProcessPhase.PARSE_CHUNK;
        try {
            // 写入解析切片阶段状态。
            saveProcessStatus(processLog, KnowledgeDocumentProcessLogStatus.RUNNING, KnowledgeDocumentProcessPhase.PARSE_CHUNK, 20, "正在解析文档并生成知识切片", null, null, null, null, null, null);
            // 记录解析切片阶段开始时间。
            long chunkStart = System.currentTimeMillis();
            // 复用已经验证过的解析切片服务，避免重复实现切片逻辑。
            ProcessKnowledgeDocumentResponse processResponse = processApplicationService.processDocument(documentId);
            // 计算解析切片耗时。
            chunkDurationMs = System.currentTimeMillis() - chunkStart;
            // 切换到向量化阶段。
            currentPhase = KnowledgeDocumentProcessPhase.EMBEDDING;
            // 写入向量化阶段状态。
            saveProcessStatus(processLog, KnowledgeDocumentProcessLogStatus.RUNNING, KnowledgeDocumentProcessPhase.EMBEDDING, 70, "正在调用 Embedding（嵌入）模型并写入 pgvector（PostgreSQL 向量扩展）", null, null, processResponse.getDocument().getChunkCount(), chunkDurationMs, null, null);
            // 记录向量化阶段开始时间。
            long embeddingStart = System.currentTimeMillis();
            // 复用已经验证过的向量化服务，把有效切片写入 pgvector（PostgreSQL 向量扩展）。
            KnowledgeDocumentEmbeddingResponse embeddingResponse = embeddingApplicationService.embedDocumentChunks(documentId);
            // 计算向量化耗时。
            embeddingDurationMs = System.currentTimeMillis() - embeddingStart;
            // 计算总耗时。
            long totalDurationMs = System.currentTimeMillis() - totalStart;
            // 标记处理日志成功。
            requiredProcessLogRepository().markSuccess(processLog.getProcessLogId(), KnowledgeDocumentProcessPhase.READY.value(), chunkDurationMs, embeddingDurationMs, totalDurationMs, embeddingResponse.getChunkCount());
            // 写入完成状态。
            saveProcessStatus(processLog, KnowledgeDocumentProcessLogStatus.SUCCESS, KnowledgeDocumentProcessPhase.READY, 100, "知识文档处理完成，当前可用于 RAG（检索增强生成）检索", null, null, embeddingResponse.getChunkCount(), chunkDurationMs, embeddingDurationMs, totalDurationMs);
            // 查询最新文档状态。
            KnowledgeDocumentEntity updatedDocument = requiredDocument(documentId);
            // 返回完整处理响应。
            return FullProcessKnowledgeDocumentResponse.builder()
                    // 设置处理日志编号。
                    .processLogId(processLog.getProcessLogId())
                    // 设置最终文档状态。
                    .document(toDocumentResponse(updatedDocument))
                    // 设置解析切片阶段响应。
                    .processResponse(processResponse)
                    // 设置切片数量。
                    .chunkCount(embeddingResponse.getChunkCount())
                    // 设置向量数量。
                    .embeddingCount(embeddingResponse.getEmbeddingCount())
                    // 设置嵌入模型供应商。
                    .embeddingProvider(embeddingResponse.getProvider())
                    // 设置嵌入模型名称。
                    .embeddingModelName(embeddingResponse.getModelName())
                    // 设置嵌入向量维度。
                    .embeddingDimension(embeddingResponse.getDimension())
                    // 设置输入词元。
                    .promptTokens(embeddingResponse.getPromptTokens())
                    // 设置总词元。
                    .totalTokens(embeddingResponse.getTotalTokens())
                    // 设置解析切片耗时。
                    .chunkDurationMs(chunkDurationMs)
                    // 设置向量化耗时。
                    .embeddingDurationMs(embeddingDurationMs)
                    // 设置总耗时。
                    .totalDurationMs(totalDurationMs)
                    // 设置处理状态。
                    .status(KnowledgeDocumentProcessLogStatus.SUCCESS.value())
                    // 构建响应。
                    .build();
        } catch (RuntimeException exception) {
            // 计算失败时的总耗时。
            long totalDurationMs = System.currentTimeMillis() - totalStart;
            // 解析异常错误码，便于处理日志复盘。
            String errorCode = resolveErrorCode(exception);
            // 记录处理日志失败，日志记录失败不覆盖原始业务异常。
            markProcessLogFailedQuietly(processLog.getProcessLogId(), currentPhase.value(), chunkDurationMs, embeddingDurationMs, totalDurationMs, errorCode, exception.getMessage());
            // 写入失败状态。
            saveProcessStatus(processLog, KnowledgeDocumentProcessLogStatus.FAILED, currentPhase, 100, "知识文档处理失败", errorCode, exception.getMessage(), null, chunkDurationMs, embeddingDurationMs, totalDurationMs);
            // 继续抛出原始异常，让全局异常处理器返回真实错误。
            throw exception;
        }
    }

    /**
     * doEmbeddingOnlyProcessDocument（仅执行向量化处理）用于失败阶段已经在 Embedding（嵌入）或 pgvector（向量入库）时复用已有切片恢复。
     *
     * @param documentId 知识文档编号
     * @param processLog 处理日志实体
     * @return 完整处理响应
     */
    private FullProcessKnowledgeDocumentResponse doEmbeddingOnlyProcessDocument(String documentId, KnowledgeDocumentProcessLogEntity processLog) {
        // 记录完整链路开始时间，用于计算总耗时。
        long totalStart = System.currentTimeMillis();
        // 当前恢复入口直接进入向量化阶段。
        KnowledgeDocumentProcessPhase currentPhase = KnowledgeDocumentProcessPhase.EMBEDDING;
        try {
            // 写入向量化阶段状态。
            saveProcessStatus(processLog, KnowledgeDocumentProcessLogStatus.RUNNING, KnowledgeDocumentProcessPhase.EMBEDDING, 70, "正在复用已有切片重新执行 Embedding（嵌入）和 pgvector（向量入库）", null, null, null, null, null, null);
            // 记录向量化阶段开始时间。
            long embeddingStart = System.currentTimeMillis();
            // 直接调用向量化服务，服务内部会读取当前文档有效切片并写入向量表。
            KnowledgeDocumentEmbeddingResponse embeddingResponse = embeddingApplicationService.embedDocumentChunks(documentId);
            // 计算向量化耗时。
            long embeddingDurationMs = System.currentTimeMillis() - embeddingStart;
            // 计算总耗时。
            long totalDurationMs = System.currentTimeMillis() - totalStart;
            // 标记处理日志成功。
            requiredProcessLogRepository().markSuccess(processLog.getProcessLogId(), KnowledgeDocumentProcessPhase.READY.value(), null, embeddingDurationMs, totalDurationMs, embeddingResponse.getChunkCount());
            // 写入完成状态。
            saveProcessStatus(processLog, KnowledgeDocumentProcessLogStatus.SUCCESS, KnowledgeDocumentProcessPhase.READY, 100, "知识文档向量化恢复完成，当前可用于 RAG（检索增强生成）检索", null, null, embeddingResponse.getChunkCount(), null, embeddingDurationMs, totalDurationMs);
            // 查询最新文档状态。
            KnowledgeDocumentEntity updatedDocument = requiredDocument(documentId);
            // 返回完整处理响应。
            return FullProcessKnowledgeDocumentResponse.builder()
                    // 设置处理日志编号。
                    .processLogId(processLog.getProcessLogId())
                    // 设置最终文档状态。
                    .document(toDocumentResponse(updatedDocument))
                    // 仅向量化恢复没有重新解析切片响应。
                    .processResponse(null)
                    // 设置切片数量。
                    .chunkCount(embeddingResponse.getChunkCount())
                    // 设置向量数量。
                    .embeddingCount(embeddingResponse.getEmbeddingCount())
                    // 设置嵌入模型供应商。
                    .embeddingProvider(embeddingResponse.getProvider())
                    // 设置嵌入模型名称。
                    .embeddingModelName(embeddingResponse.getModelName())
                    // 设置嵌入向量维度。
                    .embeddingDimension(embeddingResponse.getDimension())
                    // 设置输入词元。
                    .promptTokens(embeddingResponse.getPromptTokens())
                    // 设置总词元。
                    .totalTokens(embeddingResponse.getTotalTokens())
                    // 本次没有解析切片耗时。
                    .chunkDurationMs(null)
                    // 设置向量化耗时。
                    .embeddingDurationMs(embeddingDurationMs)
                    // 设置总耗时。
                    .totalDurationMs(totalDurationMs)
                    // 设置处理状态。
                    .status(KnowledgeDocumentProcessLogStatus.SUCCESS.value())
                    // 构建响应。
                    .build();
        } catch (RuntimeException exception) {
            // 计算失败时的总耗时。
            long totalDurationMs = System.currentTimeMillis() - totalStart;
            // 解析异常错误码。
            String errorCode = resolveErrorCode(exception);
            // 标记处理日志失败。
            markProcessLogFailedQuietly(processLog.getProcessLogId(), currentPhase.value(), null, null, totalDurationMs, errorCode, exception.getMessage());
            // 写入失败状态。
            saveProcessStatus(processLog, KnowledgeDocumentProcessLogStatus.FAILED, currentPhase, 100, "知识文档向量化恢复失败", errorCode, exception.getMessage(), null, null, null, totalDurationMs);
            // 继续抛出原始异常。
            throw exception;
        }
    }

    /**
     * createRunningProcessLog（创建运行中处理日志）用于完整处理开始前落库。
     *
     * @param document 知识文档实体
     * @return 处理日志实体
     */
    public KnowledgeDocumentProcessLogEntity createRunningProcessLog(KnowledgeDocumentEntity document, KnowledgeDocumentProcessTriggerType triggerType) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 构造运行中处理日志。
        KnowledgeDocumentProcessLogEntity entity = KnowledgeDocumentProcessLogEntity.builder()
                // 设置数据库主键。
                .id(newId("kdoc_proc_log_pk"))
                // 设置对外日志编号，保持在 64 字符以内。
                .processLogId(newId("kdoc_proc_log"))
                // 设置知识文档编号。
                .documentId(document.getDocumentId())
                // 设置知识库编号。
                .knowledgeBaseId(document.getKnowledgeBaseId())
                // 设置触发类型。
                .triggerType(triggerType == null ? KnowledgeDocumentProcessTriggerType.MANUAL.value() : triggerType.value())
                // 初始状态为运行中。
                .status(KnowledgeDocumentProcessLogStatus.RUNNING.value())
                // 初始阶段为解析切片。
                .phase(KnowledgeDocumentProcessPhase.PARSE_CHUNK.value())
                // 初始解析耗时为空。
                .parseDurationMs(null)
                // 初始切片耗时为空。
                .chunkDurationMs(null)
                // 初始嵌入耗时为空。
                .embeddingDurationMs(null)
                // 初始持久化耗时为空。
                .persistDurationMs(null)
                // 初始总耗时为空。
                .totalDurationMs(null)
                // 初始切片数量为 0。
                .chunkCount(0)
                // 初始错误码为空。
                .errorCode(null)
                // 初始错误消息为空。
                .errorMessage(null)
                // 设置开始时间。
                .startTime(now)
                // 尚未结束。
                .endTime(null)
                // 设置创建时间。
                .createTime(now)
                // 设置逻辑删除标记。
                .deleted(0)
                // 构建实体。
                .build();
        // 保存处理日志。
        return requiredProcessLogRepository().save(entity);
    }

    /**
     * saveProcessStatus（保存处理状态）用于把完整处理进度写入 Redis（缓存）。
     *
     * @param processLog 处理日志实体
     * @param status 处理状态
     * @param phase 当前阶段
     * @param progressPercent 进度百分比
     * @param message 状态消息
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param chunkCount 切片数量
     * @param chunkDurationMs 解析切片耗时
     * @param embeddingDurationMs 向量化耗时
     * @param totalDurationMs 总耗时
     */
    private void saveProcessStatus(KnowledgeDocumentProcessLogEntity processLog, KnowledgeDocumentProcessLogStatus status, KnowledgeDocumentProcessPhase phase, Integer progressPercent, String message, String errorCode, String errorMessage, Integer chunkCount, Long chunkDurationMs, Long embeddingDurationMs, Long totalDurationMs) {
        // 获取状态缓存实现，普通未启用 Redis（缓存）的测试环境可以为空。
        KnowledgeDocumentProcessStatusCache statusCache = statusCacheProvider.getIfAvailable();
        // 如果没有缓存实现，则跳过状态刷新。
        if (statusCache == null) {
            // 直接返回。
            return;
        }
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 构造状态对象。
        KnowledgeDocumentProcessStatus processStatus = KnowledgeDocumentProcessStatus.builder()
                // 设置文档编号。
                .documentId(processLog.getDocumentId())
                // 设置知识库编号。
                .knowledgeBaseId(processLog.getKnowledgeBaseId())
                // 设置处理日志编号。
                .processLogId(processLog.getProcessLogId())
                // 设置处理状态。
                .status(status)
                // 设置处理阶段。
                .phase(phase)
                // 设置触发类型。
                .triggerType(resolveTriggerType(processLog.getTriggerType()))
                // 设置进度。
                .progressPercent(progressPercent)
                // 设置消息。
                .message(message)
                // 设置错误码。
                .errorCode(errorCode)
                // 设置错误消息。
                .errorMessage(errorMessage)
                // 设置切片数量。
                .chunkCount(chunkCount)
                // 设置解析切片耗时。
                .chunkDurationMs(chunkDurationMs)
                // 设置嵌入耗时。
                .embeddingDurationMs(embeddingDurationMs)
                // 设置总耗时。
                .totalDurationMs(totalDurationMs)
                // 设置开始时间。
                .startedAt(processLog.getStartTime())
                // 设置更新时间。
                .updatedAt(now)
                // 成功或失败时设置结束时间。
                .endedAt(status == KnowledgeDocumentProcessLogStatus.RUNNING ? null : now)
                // 构建状态。
                .build();
        try {
            // 写入 Redis（缓存）。
            statusCache.save(processStatus);
        } catch (RuntimeException ignored) {
            // Redis（缓存）状态刷新属于观测能力，失败不能覆盖文档处理主链路。
        }
    }

    /**
     * resolveTriggerType（解析触发类型）用于把数据库字符串还原为触发类型枚举。
     *
     * @param triggerTypeValue 触发类型字符串
     * @return 触发类型枚举
     */
    private KnowledgeDocumentProcessTriggerType resolveTriggerType(String triggerTypeValue) {
        try {
            // 按枚举名称解析触发类型。
            return KnowledgeDocumentProcessTriggerType.valueOf(triggerTypeValue);
        } catch (RuntimeException exception) {
            // 异常值兜底为 MANUAL（手动触发）。
            return KnowledgeDocumentProcessTriggerType.MANUAL;
        }
    }

    /**
     * markProcessLogFailedQuietly（静默标记处理日志失败）用于避免日志更新失败覆盖主异常。
     *
     * @param processLogId 处理日志编号
     * @param phase 失败阶段
     * @param chunkDurationMs 解析切片耗时
     * @param embeddingDurationMs 向量化耗时
     * @param totalDurationMs 总耗时
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     */
    private void markProcessLogFailedQuietly(String processLogId, String phase, Long chunkDurationMs, Long embeddingDurationMs, Long totalDurationMs, String errorCode, String errorMessage) {
        try {
            // 更新处理日志为失败。
            requiredProcessLogRepository().markFailed(processLogId, phase, chunkDurationMs, embeddingDurationMs, totalDurationMs, errorCode, errorMessage);
        } catch (RuntimeException ignored) {
            // 日志失败不能覆盖主链路异常，后续可以接入 Trace（链路追踪）或日志系统补充观测。
        }
    }

    /**
     * resolveErrorCode（解析错误码）用于把业务异常转成处理日志可保存的错误码。
     *
     * @param exception 原始异常
     * @return 错误码文本
     */
    private String resolveErrorCode(RuntimeException exception) {
        // 如果是业务异常，则使用业务错误码。
        if (exception instanceof BusinessException businessException) {
            // 返回业务错误编码。
            return businessException.getErrorCode().getCode();
        }
        // 其他运行时异常归为内部错误。
        return ErrorCode.INTERNAL_ERROR.getCode();
    }

    /**
     * requiredDocument（获取必需文档）用于校验文档存在。
     *
     * @param documentId 知识文档编号
     * @return 知识文档实体
     */
    private KnowledgeDocumentEntity requiredDocument(String documentId) {
        // 查询知识文档实体。
        return requiredKnowledgeDocumentRepository().findByDocumentId(documentId)
                // 文档不存在时抛出明确错误。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档，documentId：" + documentId));
    }

    /**
     * requiredKnowledgeDocumentRepository（获取必需知识文档仓储）用于处理未启用数据库的场景。
     *
     * @return 知识文档仓储
     */
    private KnowledgeDocumentRepository requiredKnowledgeDocumentRepository() {
        // 从 Spring（Spring 框架）容器中获取知识文档仓储。
        KnowledgeDocumentRepository repository = knowledgeDocumentRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档完整处理接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * requiredProcessLogRepository（获取必需处理日志仓储）用于处理未启用数据库的场景。
     *
     * @return 处理日志仓储
     */
    private KnowledgeDocumentProcessLogRepository requiredProcessLogRepository() {
        // 从 Spring（Spring 框架）容器中获取处理日志仓储。
        KnowledgeDocumentProcessLogRepository repository = processLogRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档处理日志需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * toDocumentResponse（转知识文档响应）用于把数据库实体转换为接口响应对象。
     *
     * @param entity 知识文档实体
     * @return 知识文档响应
     */
    private KnowledgeDocumentResponse toDocumentResponse(KnowledgeDocumentEntity entity) {
        // 使用 Builder（建造者模式）组装接口响应。
        return KnowledgeDocumentResponse.builder()
                // 设置知识文档编号。
                .documentId(entity.getDocumentId())
                // 设置知识库编号。
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                // 设置用户编号。
                .userId(entity.getUserId())
                // 设置来源类型。
                .sourceType(entity.getSourceType())
                // 设置来源位置。
                .sourceLocation(entity.getSourceLocation())
                // 设置原始文件名。
                .originalFilename(entity.getOriginalFilename())
                // 设置存储文件名。
                .storedFilename(entity.getStoredFilename())
                // 设置文件路径。
                .filePath(entity.getFilePath())
                // 设置内容类型。
                .contentType(entity.getContentType())
                // 设置文件大小。
                .fileSize(entity.getFileSize())
                // 设置文件哈希。
                .fileHash(entity.getFileHash())
                // 设置解析状态。
                .parseStatus(entity.getParseStatus())
                // 设置切片状态。
                .chunkStatus(entity.getChunkStatus())
                // 设置向量化状态。
                .embeddingStatus(entity.getEmbeddingStatus())
                // 设置整体状态。
                .overallStatus(entity.getOverallStatus())
                // 设置错误消息。
                .errorMessage(entity.getErrorMessage())
                // 设置切片策略。
                .chunkStrategy(entity.getChunkStrategy())
                // 设置切片配置 JSON。
                .chunkConfigJson(entity.getChunkConfigJson())
                // 设置元数据 JSON。
                .metadataJson(entity.getMetadataJson())
                // 设置切片数量。
                .chunkCount(entity.getChunkCount())
                // 设置字符数。
                .charCount(entity.getCharCount())
                // 设置词元数。
                .tokenCount(entity.getTokenCount())
                // 设置启用状态。
                .enabled(entity.getEnabled())
                // 设置创建时间。
                .createTime(entity.getCreateTime())
                // 设置更新时间。
                .updateTime(entity.getUpdateTime())
                // 构建响应。
                .build();
    }

    /**
     * newId（生成编号）用于生成带业务前缀的随机编号。
     *
     * @param prefix 编号前缀
     * @return 随机编号
     */
    private String newId(String prefix) {
        // 使用 UUID（通用唯一识别码）生成本地开发阶段足够唯一的编号。
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
