package com.clipforge.application;

import com.clipforge.api.dto.AsyncProcessKnowledgeDocumentResponse;
import com.clipforge.api.dto.KnowledgeDocumentRecoveryResponse;
import com.clipforge.api.dto.RecoverStuckKnowledgeDocumentResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentProcessLogEntity;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentProcessLogStatus;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentProcessPhase;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentProcessTriggerType;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentProcessLogRepository;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentRepository;
import com.clipforge.mq.rag.document.KnowledgeDocumentProcessMessage;
import com.clipforge.mq.rag.document.KnowledgeDocumentProcessProducer;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatus;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatusCache;
import com.clipforge.rag.recovery.KnowledgeDocumentProcessErrorType;
import com.clipforge.rag.recovery.KnowledgeDocumentRecoveryProperties;
import com.clipforge.rag.recovery.KnowledgeDocumentRetryStage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KnowledgeDocumentRecoveryApplicationService（知识文档恢复应用服务）。
 *
 * <p>该服务负责 RAG（检索增强生成）知识文档处理的恢复治理，包括恢复建议查询、人工重试和卡住任务扫描。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentRecoveryApplicationService {

    /** knowledgeDocumentRepositoryProvider（知识文档仓储提供器）用于读取和更新文档状态。 */
    private final ObjectProvider<KnowledgeDocumentRepository> knowledgeDocumentRepositoryProvider;

    /** processLogRepositoryProvider（处理日志仓储提供器）用于读取和更新处理日志。 */
    private final ObjectProvider<KnowledgeDocumentProcessLogRepository> processLogRepositoryProvider;

    /** processProducerProvider（处理消息生产者提供器）用于重新投递 RocketMQ（消息队列）处理消息。 */
    private final ObjectProvider<KnowledgeDocumentProcessProducer> processProducerProvider;

    /** statusCacheProvider（状态缓存提供器）用于刷新 Redis（缓存）处理状态。 */
    private final ObjectProvider<KnowledgeDocumentProcessStatusCache> statusCacheProvider;

    /** fullProcessApplicationService（完整处理应用服务）用于创建统一的处理日志结构。 */
    private final KnowledgeDocumentFullProcessApplicationService fullProcessApplicationService;

    /** recoveryProperties（恢复配置）用于读取超时和重试次数限制。 */
    private final KnowledgeDocumentRecoveryProperties recoveryProperties;

    /**
     * queryRecovery（查询恢复建议）用于判断某个知识文档当前是否可以重试。
     *
     * @param documentId 知识文档编号
     * @return 恢复建议响应
     */
    public KnowledgeDocumentRecoveryResponse queryRecovery(String documentId) {
        // 查询文档，确保文档存在。
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        // 查询最近一次处理日志。
        KnowledgeDocumentProcessLogEntity latestLog = requiredProcessLogRepository().findLatestByDocumentId(documentId).orElse(null);
        // 统计已经发生过的消息重试次数。
        int retryCount = retryCount(documentId);
        // 构建恢复响应。
        return buildRecoveryResponse(document, latestLog, retryCount);
    }

    /**
     * retryDocumentProcess（重试知识文档处理）用于人工触发 RAG（检索增强生成）文档恢复。
     *
     * @param documentId 知识文档编号
     * @param requestedStage 请求指定的重试阶段，可以为空
     * @return 异步处理响应
     */
    @Transactional
    public AsyncProcessKnowledgeDocumentResponse retryDocumentProcess(String documentId, KnowledgeDocumentRetryStage requestedStage) {
        // 查询文档，确保文档存在。
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        // 查询最近一次处理日志，作为判断是否允许重试的依据。
        KnowledgeDocumentProcessLogEntity latestLog = requiredProcessLogRepository().findLatestByDocumentId(documentId).orElse(null);
        // 统计当前重试次数。
        int retryCount = retryCount(documentId);
        // 如果已经超过最大重试次数，则拒绝继续消耗模型成本。
        if (retryCount >= recoveryProperties.getMaxRetryTimes()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识文档重试次数已达到上限，documentId：" + documentId);
        }
        // 如果最近任务仍在运行且没有超时，则拒绝重复投递。
        if (latestLog != null && KnowledgeDocumentProcessLogStatus.RUNNING.value().equals(latestLog.getStatus()) && !isRunningStuck(latestLog)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识文档仍在处理中，请先等待当前任务完成或超时后再恢复，documentId：" + documentId);
        }
        // 如果最近任务运行中但已超时，先标记失败，再发起新的恢复任务。
        if (latestLog != null && KnowledgeDocumentProcessLogStatus.RUNNING.value().equals(latestLog.getStatus()) && isRunningStuck(latestLog)) {
            markStuckLogFailed(latestLog);
        }
        // 尝试抢占文档处理权，避免和其他处理任务并发。
        boolean marked = requiredKnowledgeDocumentRepository().markProcessingIfNotProcessing(documentId);
        // 如果抢占失败，说明其他任务刚刚开始处理。
        if (!marked) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识文档当前无法进入重试处理，可能已有任务正在运行，documentId：" + documentId);
        }
        // 推断最终重试阶段。
        KnowledgeDocumentRetryStage retryStage = requestedStage == null ? suggestedRetryStage(latestLog) : requestedStage;
        // 创建新的处理日志，触发类型固定为 MQ_RETRY（消息重试）。
        KnowledgeDocumentProcessLogEntity processLog = fullProcessApplicationService.createRunningProcessLog(document, KnowledgeDocumentProcessTriggerType.MQ_RETRY);
        // 构造重试消息。
        KnowledgeDocumentProcessMessage message = buildRetryMessage(document, processLog, retryStage, retryCount + 1);
        try {
            // 写入 Redis（缓存）运行中状态。
            saveRunningStatus(message);
            // 投递 RocketMQ（消息队列）消息。
            requiredProducer().send(message);
            // 返回异步处理响应。
            return AsyncProcessKnowledgeDocumentResponse.builder()
                    .documentId(document.getDocumentId())
                    .knowledgeBaseId(document.getKnowledgeBaseId())
                    .processLogId(processLog.getProcessLogId())
                    .messageId(message.getMessageId())
                    .status(KnowledgeDocumentProcessLogStatus.RUNNING.value())
                    .phase(phaseFromRetryStage(retryStage).value())
                    .message("知识文档恢复重试任务已提交，重试阶段：" + retryStage.value())
                    .acceptedAt(message.getCreatedAt())
                    .statusUrl("/api/knowledge-document-process-logs/" + processLog.getProcessLogId() + "/status")
                    .build();
        } catch (RuntimeException exception) {
            // 消息发送失败时标记处理日志失败。
            requiredProcessLogRepository().markFailed(processLog.getProcessLogId(), phaseFromRetryStage(retryStage).value(), null, null, 0L, KnowledgeDocumentProcessErrorType.MQ_SEND_FAILED.value(), exception.getMessage());
            // 消息发送失败时标记文档失败。
            requiredKnowledgeDocumentRepository().markFailed(documentId, "知识文档恢复消息发送失败：" + exception.getMessage());
            // 写入 Redis（缓存）失败状态。
            saveFailedStatus(message, KnowledgeDocumentProcessErrorType.MQ_SEND_FAILED, exception.getMessage());
            // 抛出业务异常。
            throw new BusinessException(ErrorCode.ASYNC_PROCESS_UNAVAILABLE, "知识文档恢复消息发送失败：" + exception.getMessage());
        }
    }

    /**
     * recoverStuckTasks（恢复卡住任务）用于扫描运行超时的知识文档处理日志并标记失败。
     *
     * @param timeoutMinutes 超时分钟数，可以为空
     * @return 扫描结果
     */
    @Transactional
    public RecoverStuckKnowledgeDocumentResponse recoverStuckTasks(Long timeoutMinutes) {
        // 解析本次扫描使用的超时阈值。
        long effectiveTimeoutMinutes = timeoutMinutes == null || timeoutMinutes <= 0 ? recoveryProperties.getProcessingTimeoutMinutes() : timeoutMinutes;
        // 计算截止时间。
        LocalDateTime beforeTime = LocalDateTime.now().minusMinutes(effectiveTimeoutMinutes);
        // 查询超时运行中的日志。
        List<KnowledgeDocumentProcessLogEntity> stuckLogs = requiredProcessLogRepository().listRunningBefore(beforeTime, recoveryProperties.getStuckScanLimit());
        // 记录被恢复的文档编号。
        List<String> recoveredDocumentIds = new ArrayList<>();
        // 遍历卡住日志。
        for (KnowledgeDocumentProcessLogEntity stuckLog : stuckLogs) {
            // 标记卡住日志失败。
            markStuckLogFailed(stuckLog);
            // 收集文档编号。
            recoveredDocumentIds.add(stuckLog.getDocumentId());
        }
        // 返回扫描结果。
        return RecoverStuckKnowledgeDocumentResponse.builder()
                .timeoutMinutes(effectiveTimeoutMinutes)
                .scannedCount(stuckLogs.size())
                .recoveredCount(recoveredDocumentIds.size())
                .recoveredDocumentIds(recoveredDocumentIds)
                .build();
    }

    /**
     * buildRecoveryResponse（构建恢复响应）用于统一状态查询和恢复详情字段。
     */
    private KnowledgeDocumentRecoveryResponse buildRecoveryResponse(KnowledgeDocumentEntity document, KnowledgeDocumentProcessLogEntity latestLog, int retryCount) {
        // 判断最近运行任务是否卡住。
        boolean runningStuck = latestLog != null && KnowledgeDocumentProcessLogStatus.RUNNING.value().equals(latestLog.getStatus()) && isRunningStuck(latestLog);
        // 判断是否可恢复。
        boolean recoverable = isRecoverable(latestLog, retryCount, runningStuck);
        // 推断建议重试阶段。
        KnowledgeDocumentRetryStage suggestedStage = suggestedRetryStage(latestLog);
        // 构造响应对象。
        return KnowledgeDocumentRecoveryResponse.builder()
                .documentId(document.getDocumentId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .latestProcessLogId(latestLog == null ? null : latestLog.getProcessLogId())
                .latestStatus(latestLog == null ? document.getOverallStatus() : latestLog.getStatus())
                .latestPhase(latestLog == null ? null : latestLog.getPhase())
                .latestErrorCode(latestLog == null ? null : latestLog.getErrorCode())
                .latestErrorMessage(latestLog == null ? document.getErrorMessage() : latestLog.getErrorMessage())
                .recoverable(recoverable)
                .retryable(recoverable)
                .runningStuck(runningStuck)
                .retryCount(retryCount)
                .maxRetryCount(recoveryProperties.getMaxRetryTimes())
                .suggestedRetryStage(suggestedStage.value())
                .recoverySuggestion(recoverySuggestion(latestLog, retryCount, recoverable, runningStuck, suggestedStage))
                .latestStartTime(latestLog == null ? null : latestLog.getStartTime())
                .latestEndTime(latestLog == null ? null : latestLog.getEndTime())
                .build();
    }

    /**
     * isRecoverable（是否可恢复）用于判断当前状态是否允许重新投递任务。
     */
    private boolean isRecoverable(KnowledgeDocumentProcessLogEntity latestLog, int retryCount, boolean runningStuck) {
        if (retryCount >= recoveryProperties.getMaxRetryTimes()) {
            return false;
        }
        if (latestLog == null) {
            return true;
        }
        if (KnowledgeDocumentProcessLogStatus.FAILED.value().equals(latestLog.getStatus())) {
            return true;
        }
        return KnowledgeDocumentProcessLogStatus.RUNNING.value().equals(latestLog.getStatus()) && runningStuck;
    }

    /**
     * isRunningStuck（运行中是否卡住）用于按开始时间判断任务是否超过恢复阈值。
     */
    private boolean isRunningStuck(KnowledgeDocumentProcessLogEntity log) {
        if (log == null || log.getStartTime() == null) {
            return false;
        }
        return Duration.between(log.getStartTime(), LocalDateTime.now()).toMinutes() >= recoveryProperties.getProcessingTimeoutMinutes();
    }

    /**
     * suggestedRetryStage（建议重试阶段）用于根据最近失败阶段推断恢复入口。
     */
    private KnowledgeDocumentRetryStage suggestedRetryStage(KnowledgeDocumentProcessLogEntity latestLog) {
        if (latestLog == null || latestLog.getPhase() == null) {
            return KnowledgeDocumentRetryStage.FULL_PROCESS;
        }
        if (KnowledgeDocumentProcessPhase.EMBEDDING.value().equals(latestLog.getPhase()) || KnowledgeDocumentProcessPhase.READY.value().equals(latestLog.getPhase())) {
            return KnowledgeDocumentRetryStage.EMBEDDING;
        }
        return KnowledgeDocumentRetryStage.FULL_PROCESS;
    }

    /**
     * recoverySuggestion（恢复建议）用于生成前端可展示的说明。
     */
    private String recoverySuggestion(KnowledgeDocumentProcessLogEntity latestLog, int retryCount, boolean recoverable, boolean runningStuck, KnowledgeDocumentRetryStage suggestedStage) {
        if (!recoverable && retryCount >= recoveryProperties.getMaxRetryTimes()) {
            return "该知识文档已达到最大重试次数，请检查文件内容、模型配置或向量库状态后再人工处理。";
        }
        if (latestLog == null) {
            return "当前没有处理日志，可以提交一次完整处理任务。";
        }
        if (runningStuck) {
            return "最近一次处理任务疑似卡住，建议先标记超时失败，再从 " + suggestedStage.value() + " 阶段重试。";
        }
        if (KnowledgeDocumentProcessLogStatus.FAILED.value().equals(latestLog.getStatus())) {
            return "最近一次处理失败，建议从 " + suggestedStage.value() + " 阶段重试。";
        }
        if (KnowledgeDocumentProcessLogStatus.SUCCESS.value().equals(latestLog.getStatus())) {
            return "最近一次处理成功，通常不需要恢复；如文档或切片发生变更，可重新处理。";
        }
        return "当前任务仍在运行中，暂不建议重复提交。";
    }

    /**
     * markStuckLogFailed（标记卡住日志失败）用于把超时运行中任务恢复为可见失败状态。
     */
    private void markStuckLogFailed(KnowledgeDocumentProcessLogEntity stuckLog) {
        // 计算总耗时。
        long totalDurationMs = stuckLog.getStartTime() == null ? 0L : Duration.between(stuckLog.getStartTime(), LocalDateTime.now()).toMillis();
        // 标记日志失败。
        requiredProcessLogRepository().markFailed(stuckLog.getProcessLogId(), safePhase(stuckLog.getPhase()), stuckLog.getChunkDurationMs(), stuckLog.getEmbeddingDurationMs(), totalDurationMs, KnowledgeDocumentProcessErrorType.TASK_TIMEOUT.value(), "知识文档处理任务运行超时，已标记为失败，可重新触发恢复。");
        // 标记文档失败。
        requiredKnowledgeDocumentRepository().markFailed(stuckLog.getDocumentId(), "知识文档处理任务运行超时，已标记为失败，可重新触发恢复。");
        // 写入缓存失败状态。
        saveFailedStatus(stuckLog, KnowledgeDocumentProcessErrorType.TASK_TIMEOUT, "知识文档处理任务运行超时，已标记为失败，可重新触发恢复。", totalDurationMs);
    }

    /**
     * buildRetryMessage（构造重试消息）用于重新投递 RocketMQ（消息队列）处理任务。
     */
    private KnowledgeDocumentProcessMessage buildRetryMessage(KnowledgeDocumentEntity document, KnowledgeDocumentProcessLogEntity processLog, KnowledgeDocumentRetryStage retryStage, int retryTimes) {
        return KnowledgeDocumentProcessMessage.builder()
                .messageId("kdoc_retry_msg_" + shortUuid())
                .documentId(document.getDocumentId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .processLogId(processLog.getProcessLogId())
                .triggerType(KnowledgeDocumentProcessTriggerType.MQ_RETRY)
                .retryTimes(retryTimes)
                .retryStage(retryStage)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * saveRunningStatus（保存运行中状态）用于让恢复任务投递后立刻可查询。
     */
    private void saveRunningStatus(KnowledgeDocumentProcessMessage message) {
        KnowledgeDocumentProcessStatusCache statusCache = statusCacheProvider.getIfAvailable();
        if (statusCache == null) {
            return;
        }
        try {
            statusCache.save(KnowledgeDocumentProcessStatus.builder()
                    .documentId(message.getDocumentId())
                    .knowledgeBaseId(message.getKnowledgeBaseId())
                    .processLogId(message.getProcessLogId())
                    .status(KnowledgeDocumentProcessLogStatus.RUNNING)
                    .phase(phaseFromRetryStage(message.getRetryStage()))
                    .triggerType(message.getTriggerType())
                    .progressPercent(message.getRetryStage() == KnowledgeDocumentRetryStage.EMBEDDING || message.getRetryStage() == KnowledgeDocumentRetryStage.PERSIST_VECTOR ? 60 : 10)
                    .message("知识文档恢复重试任务已启动，重试阶段：" + message.getRetryStage().value())
                    .startedAt(message.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .build());
        } catch (RuntimeException ignored) {
            // Redis（缓存）状态写入失败不能阻断恢复消息投递。
        }
    }

    /**
     * saveFailedStatus（保存失败状态）用于恢复消息发送失败时写入 Redis（缓存）。
     */
    private void saveFailedStatus(KnowledgeDocumentProcessMessage message, KnowledgeDocumentProcessErrorType errorType, String errorMessage) {
        saveFailedStatus(message.getDocumentId(), message.getKnowledgeBaseId(), message.getProcessLogId(), phaseFromRetryStage(message.getRetryStage()), message.getTriggerType(), message.getCreatedAt(), errorType, errorMessage, 0L);
    }

    /**
     * saveFailedStatus（保存失败状态）用于卡住任务扫描时写入 Redis（缓存）。
     */
    private void saveFailedStatus(KnowledgeDocumentProcessLogEntity log, KnowledgeDocumentProcessErrorType errorType, String errorMessage, Long totalDurationMs) {
        saveFailedStatus(log.getDocumentId(), log.getKnowledgeBaseId(), log.getProcessLogId(), parsePhase(log.getPhase()), parseTriggerType(log.getTriggerType()), log.getStartTime(), errorType, errorMessage, totalDurationMs);
    }

    /**
     * saveFailedStatus（保存失败状态）用于统一构造失败缓存对象。
     */
    private void saveFailedStatus(String documentId, String knowledgeBaseId, String processLogId, KnowledgeDocumentProcessPhase phase, KnowledgeDocumentProcessTriggerType triggerType, LocalDateTime startedAt, KnowledgeDocumentProcessErrorType errorType, String errorMessage, Long totalDurationMs) {
        KnowledgeDocumentProcessStatusCache statusCache = statusCacheProvider.getIfAvailable();
        if (statusCache == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            statusCache.save(KnowledgeDocumentProcessStatus.builder()
                    .documentId(documentId)
                    .knowledgeBaseId(knowledgeBaseId)
                    .processLogId(processLogId)
                    .status(KnowledgeDocumentProcessLogStatus.FAILED)
                    .phase(phase)
                    .triggerType(triggerType)
                    .progressPercent(100)
                    .message("知识文档处理恢复失败")
                    .errorCode(errorType.value())
                    .errorMessage(errorMessage)
                    .startedAt(startedAt)
                    .updatedAt(now)
                    .endedAt(now)
                    .totalDurationMs(totalDurationMs)
                    .build());
        } catch (RuntimeException ignored) {
            // Redis（缓存）失败状态写入失败不能覆盖主流程异常。
        }
    }

    /**
     * retryCount（重试次数）用于从处理日志统计 MQ_RETRY（消息重试）触发次数。
     */
    private int retryCount(String documentId) {
        return Math.toIntExact(requiredProcessLogRepository().countByDocumentIdAndTriggerType(documentId, KnowledgeDocumentProcessTriggerType.MQ_RETRY.value()));
    }

    /**
     * phaseFromRetryStage（由重试阶段推断处理阶段）用于消息和接口响应。
     */
    private KnowledgeDocumentProcessPhase phaseFromRetryStage(KnowledgeDocumentRetryStage retryStage) {
        if (retryStage == KnowledgeDocumentRetryStage.EMBEDDING || retryStage == KnowledgeDocumentRetryStage.PERSIST_VECTOR) {
            return KnowledgeDocumentProcessPhase.EMBEDDING;
        }
        return KnowledgeDocumentProcessPhase.PARSE_CHUNK;
    }

    /**
     * parsePhase（解析处理阶段）用于从数据库字符串转换为枚举。
     */
    private KnowledgeDocumentProcessPhase parsePhase(String phase) {
        try {
            return KnowledgeDocumentProcessPhase.valueOf(phase);
        } catch (RuntimeException exception) {
            return KnowledgeDocumentProcessPhase.PARSE_CHUNK;
        }
    }

    /**
     * parseTriggerType（解析触发类型）用于从数据库字符串转换为枚举。
     */
    private KnowledgeDocumentProcessTriggerType parseTriggerType(String triggerType) {
        try {
            return KnowledgeDocumentProcessTriggerType.valueOf(triggerType);
        } catch (RuntimeException exception) {
            return KnowledgeDocumentProcessTriggerType.MANUAL;
        }
    }

    /**
     * safePhase（安全阶段）用于避免空阶段写入数据库。
     */
    private String safePhase(String phase) {
        return phase == null || phase.isBlank() ? KnowledgeDocumentProcessPhase.PARSE_CHUNK.value() : phase;
    }

    /**
     * requiredDocument（获取必需文档）用于校验文档存在。
     */
    private KnowledgeDocumentEntity requiredDocument(String documentId) {
        return requiredKnowledgeDocumentRepository().findByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档，documentId：" + documentId));
    }

    /**
     * requiredProducer（获取必需生产者）用于确保 RocketMQ（消息队列）异步能力可用。
     */
    private KnowledgeDocumentProcessProducer requiredProducer() {
        KnowledgeDocumentProcessProducer producer = processProducerProvider.getIfAvailable();
        if (producer == null) {
            throw new BusinessException(ErrorCode.ASYNC_PROCESS_UNAVAILABLE, "知识文档恢复需要启用 rocketmq Profile（消息队列配置环境）");
        }
        return producer;
    }

    /**
     * requiredKnowledgeDocumentRepository（获取必需知识文档仓储）用于处理未启用数据库的场景。
     */
    private KnowledgeDocumentRepository requiredKnowledgeDocumentRepository() {
        KnowledgeDocumentRepository repository = knowledgeDocumentRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档恢复需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    /**
     * requiredProcessLogRepository（获取必需处理日志仓储）用于处理未启用数据库的场景。
     */
    private KnowledgeDocumentProcessLogRepository requiredProcessLogRepository() {
        KnowledgeDocumentProcessLogRepository repository = processLogRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档恢复需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    /**
     * shortUuid（短随机编号）用于生成较短的业务消息编号。
     */
    private String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
