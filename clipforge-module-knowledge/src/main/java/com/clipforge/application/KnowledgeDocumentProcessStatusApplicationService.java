package com.clipforge.application;

import com.clipforge.contract.knowledge.KnowledgeDocumentProcessStatusResponse;
import com.clipforge.contract.knowledge.KnowledgeDocumentRecoveryResponse;
import com.clipforge.knowledge.application.api.KnowledgeDocumentProcessStatusQueryUseCase;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentProcessLogEntity;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentOverallStatus;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessLogStatus;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessPhase;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentProcessLogRepository;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentRepository;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatus;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatusCache;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * KnowledgeDocumentProcessStatusApplicationService（知识文档处理状态应用服务）。
 *
 * <p>该服务负责查询知识文档处理状态，优先读取 Redis（缓存），缓存未命中时回查 PostgreSQL（关系数据库）处理日志。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentProcessStatusApplicationService implements KnowledgeDocumentProcessStatusQueryUseCase {

    /** SOURCE_REDIS（Redis 来源）表示状态直接来自 Redis（缓存）。 */
    private static final String SOURCE_REDIS = "REDIS";

    /** SOURCE_DATABASE（数据库来源）表示状态来自处理日志表。 */
    private static final String SOURCE_DATABASE = "DATABASE";

    /** SOURCE_DOCUMENT（文档来源）表示状态来自文档表兜底。 */
    private static final String SOURCE_DOCUMENT = "DOCUMENT";

    /** statusCacheProvider（状态缓存提供器）用于可选获取 Redis（缓存）状态缓存实现。 */
    private final ObjectProvider<KnowledgeDocumentProcessStatusCache> statusCacheProvider;

    /** processLogRepositoryProvider（处理日志仓储提供器）用于回查数据库处理日志。 */
    private final ObjectProvider<KnowledgeDocumentProcessLogRepository> processLogRepositoryProvider;

    /** knowledgeDocumentRepositoryProvider（知识文档仓储提供器）用于文档状态兜底。 */
    private final ObjectProvider<KnowledgeDocumentRepository> knowledgeDocumentRepositoryProvider;

    /** recoveryApplicationServiceProvider（恢复应用服务提供器）用于给状态响应补充恢复建议。 */
    private final ObjectProvider<KnowledgeDocumentRecoveryApplicationService> recoveryApplicationServiceProvider;

    /**
     * queryByDocumentId（按文档编号查询状态）用于读取某个知识文档最近一次处理状态。
     *
     * @param documentId 知识文档编号
     * @return 处理状态响应
     */
    public KnowledgeDocumentProcessStatusResponse queryByDocumentId(String documentId) {
        // 优先从 Redis（缓存）读取状态。
        KnowledgeDocumentProcessStatusCache statusCache = statusCacheProvider.getIfAvailable();
        // 如果缓存实现存在，则尝试按文档编号读取。
        if (statusCache != null) {
            // 命中缓存时直接转换响应。
            java.util.Optional<KnowledgeDocumentProcessStatus> cachedStatus = statusCache.findByDocumentId(documentId);
            // 如果缓存存在，则返回 Redis 来源响应。
            if (cachedStatus.isPresent()) {
                // 转换缓存状态为接口响应。
                return enrichRecovery(fromCache(cachedStatus.get()));
            }
        }
        // Redis（缓存）未命中时回查最近处理日志。
        return requiredProcessLogRepository().findLatestByDocumentId(documentId)
                // 如果存在处理日志，则转换为数据库来源响应。
                .map(this::fromProcessLog)
                .map(this::enrichRecovery)
                // 如果没有处理日志，则使用知识文档状态兜底。
                .orElseGet(() -> enrichRecovery(fromDocument(requiredDocument(documentId))));
    }

    /**
     * queryByProcessLogId（按处理日志编号查询状态）用于读取某一次处理任务的状态。
     *
     * @param processLogId 处理日志编号
     * @return 处理状态响应
     */
    public KnowledgeDocumentProcessStatusResponse queryByProcessLogId(String processLogId) {
        // 优先从 Redis（缓存）读取状态。
        KnowledgeDocumentProcessStatusCache statusCache = statusCacheProvider.getIfAvailable();
        // 如果缓存实现存在，则尝试按处理日志编号读取。
        if (statusCache != null) {
            // 命中缓存时直接转换响应。
            java.util.Optional<KnowledgeDocumentProcessStatus> cachedStatus = statusCache.findByProcessLogId(processLogId);
            // 如果缓存存在，则返回 Redis 来源响应。
            if (cachedStatus.isPresent()) {
                // 转换缓存状态为接口响应。
                return enrichRecovery(fromCache(cachedStatus.get()));
            }
        }
        // Redis（缓存）未命中时回查处理日志表。
        return requiredProcessLogRepository().findByProcessLogId(processLogId)
                // 如果存在处理日志，则转换为数据库来源响应。
                .map(this::fromProcessLog)
                .map(this::enrichRecovery)
                // 处理日志不存在时抛出明确错误。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档处理日志，processLogId：" + processLogId));
    }

    /**
     * enrichRecovery（补充恢复建议）用于把恢复治理信息合并到状态查询响应中。
     *
     * @param response 原始状态响应
     * @return 补充恢复建议后的状态响应
     */
    private KnowledgeDocumentProcessStatusResponse enrichRecovery(KnowledgeDocumentProcessStatusResponse response) {
        // 如果响应为空或缺少文档编号，则无法补充恢复建议。
        if (response == null || response.getDocumentId() == null || response.getDocumentId().isBlank()) {
            // 返回原始响应。
            return response;
        }
        // 获取恢复应用服务。
        KnowledgeDocumentRecoveryApplicationService recoveryApplicationService = recoveryApplicationServiceProvider.getIfAvailable();
        // 如果恢复应用服务不可用，则返回原始响应。
        if (recoveryApplicationService == null) {
            // 返回原始响应。
            return response;
        }
        try {
            // 查询恢复建议。
            KnowledgeDocumentRecoveryResponse recovery = recoveryApplicationService.queryRecovery(response.getDocumentId());
            // 设置是否可恢复。
            response.setRecoverable(recovery.getRecoverable());
            // 设置是否可重试。
            response.setRetryable(recovery.getRetryable());
            // 设置运行中是否卡住。
            response.setRunningStuck(recovery.getRunningStuck());
            // 设置已重试次数。
            response.setRetryCount(recovery.getRetryCount());
            // 设置最大重试次数。
            response.setMaxRetryCount(recovery.getMaxRetryCount());
            // 设置建议重试阶段。
            response.setSuggestedRetryStage(recovery.getSuggestedRetryStage());
            // 设置恢复建议。
            response.setRecoverySuggestion(recovery.getRecoverySuggestion());
        } catch (RuntimeException ignored) {
            // 恢复建议属于增强信息，失败不能影响基础状态查询。
        }
        // 返回增强后的响应。
        return response;
    }

    /**
     * fromCache（从缓存转换）用于把 Redis（缓存）状态对象转换为接口响应。
     *
     * @param status 缓存状态
     * @return 处理状态响应
     */
    private KnowledgeDocumentProcessStatusResponse fromCache(KnowledgeDocumentProcessStatus status) {
        // 使用 Builder（建造者模式）组装响应。
        return KnowledgeDocumentProcessStatusResponse.builder()
                // 设置文档编号。
                .documentId(status.getDocumentId())
                // 设置知识库编号。
                .knowledgeBaseId(status.getKnowledgeBaseId())
                // 设置处理日志编号。
                .processLogId(status.getProcessLogId())
                // 设置处理状态。
                .status(status.statusValue())
                // 设置处理阶段。
                .phase(status.phaseValue())
                // 设置触发类型。
                .triggerType(status.triggerTypeValue())
                // 设置进度。
                .progressPercent(status.getProgressPercent())
                // 设置状态消息。
                .message(status.getMessage())
                // 设置错误码。
                .errorCode(status.getErrorCode())
                // 设置错误消息。
                .errorMessage(status.getErrorMessage())
                // 设置切片数量。
                .chunkCount(status.getChunkCount())
                // 设置解析切片耗时。
                .chunkDurationMs(status.getChunkDurationMs())
                // 设置嵌入耗时。
                .embeddingDurationMs(status.getEmbeddingDurationMs())
                // 设置总耗时。
                .totalDurationMs(status.getTotalDurationMs())
                // 设置开始时间。
                .startedAt(status.getStartedAt())
                // 设置更新时间。
                .updatedAt(status.getUpdatedAt())
                // 设置结束时间。
                .endedAt(status.getEndedAt())
                // 设置状态来源。
                .source(SOURCE_REDIS)
                // 标记命中缓存。
                .cacheHit(true)
                // 构建响应。
                .build();
    }

    /**
     * fromProcessLog（从处理日志转换）用于把数据库处理日志转换为接口响应。
     *
     * @param log 处理日志实体
     * @return 处理状态响应
     */
    private KnowledgeDocumentProcessStatusResponse fromProcessLog(KnowledgeDocumentProcessLogEntity log) {
        // 使用 Builder（建造者模式）组装响应。
        return KnowledgeDocumentProcessStatusResponse.builder()
                // 设置文档编号。
                .documentId(log.getDocumentId())
                // 设置知识库编号。
                .knowledgeBaseId(log.getKnowledgeBaseId())
                // 设置处理日志编号。
                .processLogId(log.getProcessLogId())
                // 设置处理状态。
                .status(log.getStatus())
                // 设置处理阶段。
                .phase(log.getPhase())
                // 设置触发类型。
                .triggerType(log.getTriggerType())
                // 由数据库日志粗略推断进度。
                .progressPercent(progressFromLog(log))
                // 设置状态消息。
                .message(messageFromLog(log))
                // 设置错误码。
                .errorCode(log.getErrorCode())
                // 设置错误消息。
                .errorMessage(log.getErrorMessage())
                // 设置切片数量。
                .chunkCount(log.getChunkCount())
                // 设置解析切片耗时。
                .chunkDurationMs(log.getChunkDurationMs())
                // 设置嵌入耗时。
                .embeddingDurationMs(log.getEmbeddingDurationMs())
                // 设置总耗时。
                .totalDurationMs(log.getTotalDurationMs())
                // 设置开始时间。
                .startedAt(log.getStartTime())
                // 数据库日志没有单独更新时间，这里用结束时间或创建时间兜底。
                .updatedAt(log.getEndTime() != null ? log.getEndTime() : log.getCreateTime())
                // 设置结束时间。
                .endedAt(log.getEndTime())
                // 设置状态来源。
                .source(SOURCE_DATABASE)
                // 标记未命中缓存。
                .cacheHit(false)
                // 构建响应。
                .build();
    }

    /**
     * fromDocument（从文档转换）用于没有处理日志时返回文档当前状态兜底响应。
     *
     * @param document 知识文档实体
     * @return 处理状态响应
     */
    private KnowledgeDocumentProcessStatusResponse fromDocument(KnowledgeDocumentEntity document) {
        // 使用 Builder（建造者模式）组装响应。
        return KnowledgeDocumentProcessStatusResponse.builder()
                // 设置文档编号。
                .documentId(document.getDocumentId())
                // 设置知识库编号。
                .knowledgeBaseId(document.getKnowledgeBaseId())
                // 文档兜底没有处理日志编号。
                .processLogId(null)
                // 使用文档整体状态作为状态展示。
                .status(document.getOverallStatus())
                // 文档兜底没有明确处理阶段。
                .phase(null)
                // 文档兜底没有触发类型。
                .triggerType(null)
                // 根据文档整体状态粗略估算进度。
                .progressPercent(progressFromDocument(document))
                // 设置状态消息。
                .message("当前没有处理日志，返回知识文档表中的整体状态")
                // 设置错误消息。
                .errorMessage(document.getErrorMessage())
                // 设置切片数量。
                .chunkCount(document.getChunkCount())
                // 文档兜底没有阶段耗时。
                .chunkDurationMs(null)
                // 文档兜底没有阶段耗时。
                .embeddingDurationMs(null)
                // 文档兜底没有总耗时。
                .totalDurationMs(null)
                // 文档兜底使用创建时间作为开始时间。
                .startedAt(document.getCreateTime())
                // 文档兜底使用更新时间作为更新时间。
                .updatedAt(document.getUpdateTime())
                // 文档兜底没有结束时间。
                .endedAt(document.getLastProcessTime())
                // 设置状态来源。
                .source(SOURCE_DOCUMENT)
                // 标记未命中缓存。
                .cacheHit(false)
                // 构建响应。
                .build();
    }

    /**
     * progressFromLog（从日志推断进度）用于数据库回查时提供粗粒度进度。
     *
     * @param log 处理日志
     * @return 进度百分比
     */
    private Integer progressFromLog(KnowledgeDocumentProcessLogEntity log) {
        // 成功或跳过视为完成。
        if (KnowledgeDocumentProcessLogStatus.SUCCESS.value().equals(log.getStatus()) || KnowledgeDocumentProcessLogStatus.SKIPPED.value().equals(log.getStatus())) {
            // 返回 100。
            return 100;
        }
        // 失败视为处理结束但失败。
        if (KnowledgeDocumentProcessLogStatus.FAILED.value().equals(log.getStatus())) {
            // 返回 100。
            return 100;
        }
        // 嵌入阶段表示已经过半。
        if (KnowledgeDocumentProcessPhase.EMBEDDING.value().equals(log.getPhase())) {
            // 返回 70。
            return 70;
        }
        // 默认运行中解析切片阶段。
        return 30;
    }

    /**
     * progressFromDocument（从文档推断进度）用于文档状态兜底。
     *
     * @param document 知识文档实体
     * @return 进度百分比
     */
    private Integer progressFromDocument(KnowledgeDocumentEntity document) {
        // 可检索或失败都表示当前处理已经结束。
        if (KnowledgeDocumentOverallStatus.READY.value().equals(document.getOverallStatus()) || KnowledgeDocumentOverallStatus.FAILED.value().equals(document.getOverallStatus())) {
            // 返回 100。
            return 100;
        }
        // 已切片表示向量化还未完成。
        if (KnowledgeDocumentOverallStatus.CHUNKED.value().equals(document.getOverallStatus())) {
            // 返回 60。
            return 60;
        }
        // 处理中表示粗略进度。
        if (KnowledgeDocumentOverallStatus.PROCESSING.value().equals(document.getOverallStatus())) {
            // 返回 30。
            return 30;
        }
        // 待处理返回 0。
        return 0;
    }

    /**
     * messageFromLog（从处理日志生成消息）用于给数据库回查结果补充可读说明。
     *
     * @param log 处理日志
     * @return 状态消息
     */
    private String messageFromLog(KnowledgeDocumentProcessLogEntity log) {
        // 失败时优先返回错误消息。
        if (KnowledgeDocumentProcessLogStatus.FAILED.value().equals(log.getStatus())) {
            // 返回失败说明。
            return log.getErrorMessage() == null || log.getErrorMessage().isBlank() ? "知识文档处理失败" : log.getErrorMessage();
        }
        // 成功时返回完成说明。
        if (KnowledgeDocumentProcessLogStatus.SUCCESS.value().equals(log.getStatus())) {
            // 返回成功说明。
            return "知识文档处理完成，当前可用于 RAG（检索增强生成）检索";
        }
        // 默认返回运行中说明。
        return "知识文档正在处理，当前阶段：" + log.getPhase();
    }

    /**
     * requiredDocument（获取必需文档）用于文档状态兜底。
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
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档处理状态查询需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return repository;
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
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档处理状态查询需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return repository;
    }
}
