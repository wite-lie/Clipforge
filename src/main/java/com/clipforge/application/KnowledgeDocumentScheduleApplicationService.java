package com.clipforge.application;

import com.clipforge.api.dto.KnowledgeUrlSyncExecutionResponse;
import com.clipforge.api.dto.KnowledgeUrlSyncScheduleResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentProcessLogEntity;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentScheduleEntity;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentScheduleExecEntity;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentProcessTriggerType;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentScheduleExecStatus;
import com.clipforge.infra.persistence.enums.KnowledgeDocumentSourceType;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentRepository;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentScheduleExecRepository;
import com.clipforge.infra.persistence.repository.KnowledgeDocumentScheduleRepository;
import com.clipforge.mq.rag.urlsync.KnowledgeUrlSyncMessage;
import com.clipforge.mq.rag.urlsync.KnowledgeUrlSyncProducer;
import com.clipforge.rag.KnowledgeStoredFile;
import com.clipforge.rag.KnowledgeUrlFileStorage;
import com.clipforge.rag.sync.KnowledgeDocumentUrlSyncProperties;
import com.clipforge.rag.sync.UrlContentFetcher;
import com.clipforge.rag.sync.UrlContentFingerprintCalculator;
import com.clipforge.rag.sync.UrlContentSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KnowledgeDocumentScheduleApplicationService（知识文档 URL 定时同步应用服务）。
 *
 * <p>该服务负责 URL（统一资源定位符）来源知识文档的调度、变更检测、租约控制和重新处理触发。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentScheduleApplicationService {

    /** knowledgeDocumentRepositoryProvider（知识文档仓储提供器）用于读取和刷新 URL 来源文档。 */
    private final ObjectProvider<KnowledgeDocumentRepository> knowledgeDocumentRepositoryProvider;

    /** scheduleRepositoryProvider（定时任务仓储提供器）用于管理 URL 同步任务和数据库租约。 */
    private final ObjectProvider<KnowledgeDocumentScheduleRepository> scheduleRepositoryProvider;

    /** execRepositoryProvider（执行记录仓储提供器）用于保存每次 URL 同步历史。 */
    private final ObjectProvider<KnowledgeDocumentScheduleExecRepository> execRepositoryProvider;

    /** urlSyncProducerProvider（URL 同步生产者提供器）用于可选投递 RocketMQ（消息队列）任务。 */
    private final ObjectProvider<KnowledgeUrlSyncProducer> urlSyncProducerProvider;

    /** fullProcessApplicationService（完整处理服务）用于 URL 内容变化后复用解析、切片、向量化链路。 */
    private final KnowledgeDocumentFullProcessApplicationService fullProcessApplicationService;

    /** urlContentFetcher（URL 内容抓取器）用于抓取远程文档快照。 */
    private final UrlContentFetcher urlContentFetcher;

    /** urlFileStorage（URL 文件存储器）用于把远程内容保存到本地文件。 */
    private final KnowledgeUrlFileStorage urlFileStorage;

    /** fingerprintCalculator（指纹计算器）用于 ETag、Last-Modified 和内容哈希变更判断。 */
    private final UrlContentFingerprintCalculator fingerprintCalculator;

    /** properties（URL 同步配置）用于控制扫描、租约、MQ 和抓取限制。 */
    private final KnowledgeDocumentUrlSyncProperties properties;

    /**
     * executeDueSchedules（执行到期任务）用于扫描当前批次中需要执行的 URL 同步任务。
     */
    public void executeDueSchedules() {
        if (!properties.isEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<KnowledgeDocumentScheduleEntity> dueSchedules = requiredScheduleRepository().listDueSchedules(now, properties.getScanBatchSize());
        for (KnowledgeDocumentScheduleEntity schedule : dueSchedules) {
            try {
                dispatchOrExecuteDueSchedule(schedule, now);
            } catch (RuntimeException ignored) {
                // 单个任务失败不能中断整批扫描。
            }
        }
    }

    /**
     * upsertScheduleForDocument（保存文档定时任务）用于为一个 URL 来源文档创建或更新同步配置。
     *
     * @param documentId knowledge document id
     * @param scheduleCron cron expression
     * @param enabled whether the schedule is enabled
     * @return saved schedule entity
     */
    @Transactional
    public KnowledgeDocumentScheduleEntity upsertScheduleForDocument(String documentId, String scheduleCron, boolean enabled) {
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        if (!KnowledgeDocumentSourceType.URL.value().equals(document.getSourceType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有 URL 来源文档才能创建同步任务");
        }
        KnowledgeDocumentScheduleRepository scheduleRepository = requiredScheduleRepository();
        KnowledgeDocumentRepository documentRepository = requiredKnowledgeDocumentRepository();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextSyncTime = nextSyncTime(scheduleCron, now);
        documentRepository.updateScheduleConfig(documentId, enabled ? 1 : 0, scheduleCron);
        Optional<KnowledgeDocumentScheduleEntity> existing = scheduleRepository.findByDocumentId(documentId);
        if (existing.isPresent()) {
            KnowledgeDocumentScheduleEntity schedule = existing.get();
            return scheduleRepository.updateDefinition(schedule.getScheduleId(), document.getSourceLocation(), scheduleCron, enabled ? 1 : 0, nextSyncTime);
        }
        KnowledgeDocumentScheduleEntity entity = KnowledgeDocumentScheduleEntity.builder()
                .id(newId("knowledge_document_schedule_pk"))
                .scheduleId(newId("knowledge_document_schedule"))
                .documentId(document.getDocumentId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .sourceLocation(document.getSourceLocation())
                .scheduleCron(scheduleCron)
                .enabled(enabled ? 1 : 0)
                .nextSyncTime(nextSyncTime)
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        return scheduleRepository.save(entity);
    }

    /**
     * upsertScheduleResponseForDocument（保存并返回同步任务响应）用于接口层创建或更新 URL 同步任务。
     */
    public KnowledgeUrlSyncScheduleResponse upsertScheduleResponseForDocument(String documentId, String scheduleCron, boolean enabled) {
        return toScheduleResponse(upsertScheduleForDocument(documentId, scheduleCron, enabled));
    }

    /**
     * queryScheduleByDocumentId（按文档查询同步任务）用于接口层读取 URL 同步配置。
     */
    public KnowledgeUrlSyncScheduleResponse queryScheduleByDocumentId(String documentId) {
        KnowledgeDocumentScheduleEntity entity = requiredScheduleRepository().findByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到 URL 同步任务，documentId：" + documentId));
        return toScheduleResponse(entity);
    }

    /**
     * listSchedulesByKnowledgeBaseId（按知识库查询同步任务）用于接口层读取 URL 同步任务列表。
     */
    public List<KnowledgeUrlSyncScheduleResponse> listSchedulesByKnowledgeBaseId(String knowledgeBaseId) {
        return requiredScheduleRepository().listByKnowledgeBaseId(knowledgeBaseId).stream()
                .map(this::toScheduleResponse)
                .toList();
    }

    /**
     * executeSingleScheduleResponse（执行单个任务并返回响应）用于接口层手动触发 URL 同步。
     */
    public KnowledgeUrlSyncExecutionResponse executeSingleScheduleResponse(String scheduleId) {
        return toExecutionResponse(executeSingleSchedule(scheduleId));
    }

    /**
     * listExecutions（查询执行历史）用于接口层读取 URL 同步历史。
     */
    public List<KnowledgeUrlSyncExecutionResponse> listExecutions(String scheduleId, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        return requiredExecRepository().listByScheduleId(scheduleId, safeLimit).stream()
                .map(this::toExecutionResponse)
                .toList();
    }

    /**
     * executeSingleSchedule（执行单个任务）用于手动触发或同步模式下执行 URL 同步。
     *
     * @param scheduleId 定时任务编号
     * @return 执行记录实体
     */
    public KnowledgeDocumentScheduleExecEntity executeSingleSchedule(String scheduleId) {
        KnowledgeDocumentScheduleRepository scheduleRepository = requiredScheduleRepository();
        KnowledgeDocumentScheduleEntity schedule = requiredSchedule(scheduleId);
        LocalDateTime now = LocalDateTime.now();
        String leaseOwner = buildLeaseOwner();
        LocalDateTime leaseUntil = now.plusMinutes(properties.getLeaseMinutes());
        if (!scheduleRepository.acquireLease(scheduleId, leaseOwner, leaseUntil, now)) {
            return createSkippedExec(schedule, "同步任务正在被其他实例处理");
        }
        return executeLeasedSchedule(scheduleId);
    }

    /**
     * executeLeasedSchedule（执行已持有租约任务）用于 MQ（消息队列）消费者或本机调度在租约已获得后真正执行同步。
     *
     * @param scheduleId 定时任务编号
     * @return 执行记录
     */
    public KnowledgeDocumentScheduleExecEntity executeLeasedSchedule(String scheduleId) {
        KnowledgeDocumentScheduleRepository scheduleRepository = requiredScheduleRepository();
        KnowledgeDocumentScheduleExecRepository execRepository = requiredExecRepository();
        KnowledgeDocumentScheduleEntity schedule = requiredSchedule(scheduleId);
        KnowledgeDocumentScheduleExecEntity exec = createRunningExec(schedule);
        long syncStart = System.currentTimeMillis();
        try {
            UrlContentSnapshot snapshot = urlContentFetcher.fetchIfChanged(schedule.getSourceLocation(), schedule.getLastEtag(), schedule.getLastModified());
            long fetchDurationMs = snapshot.getFetchDurationMs() == null ? 0L : snapshot.getFetchDurationMs();
            if (Boolean.TRUE.equals(snapshot.getNotModified()) || fingerprintCalculator.sameContent(schedule.getLastEtag(), schedule.getLastModified(), schedule.getLastContentHash(), snapshot)) {
                LocalDateTime finishedAt = LocalDateTime.now();
                LocalDateTime nextTime = nextSyncTime(schedule.getScheduleCron(), finishedAt);
                scheduleRepository.markSkipped(schedule.getScheduleId(), finishedAt, nextTime);
                execRepository.markSkipped(exec.getExecId(), snapshot.getEtag(), snapshot.getLastModified(), snapshot.getContentHash(), snapshot.getContentLength(), fetchDurationMs, System.currentTimeMillis() - syncStart, "URL 内容未变化，已跳过切片和向量化");
                return execRepository.findByExecId(exec.getExecId()).orElseThrow();
            }
            KnowledgeStoredFile storedFile = urlFileStorage.store(schedule.getDocumentId(), snapshot.getSuggestedFilename(), snapshot.getContentType(), snapshot.getContentBytes());
            KnowledgeDocumentRepository documentRepository = requiredKnowledgeDocumentRepository();
            KnowledgeDocumentEntity refreshedDocument = documentRepository.refreshRemoteSourceDocument(
                    schedule.getDocumentId(),
                    schedule.getSourceLocation(),
                    snapshot.getSuggestedFilename(),
                    storedFile.getStoredFilename(),
                    storedFile.getFilePath(),
                    snapshot.getContentType(),
                    snapshot.getContentLength(),
                    storedFile.getFileHash(),
                    snapshot.getEtag(),
                    snapshot.getLastModified(),
                    snapshot.getContentHash());
            KnowledgeDocumentProcessLogEntity processLog = fullProcessApplicationService.createRunningProcessLog(refreshedDocument, KnowledgeDocumentProcessTriggerType.SCHEDULE);
            fullProcessApplicationService.fullProcessDocumentWithExistingLog(refreshedDocument.getDocumentId(), processLog.getProcessLogId());
            LocalDateTime finishedAt = LocalDateTime.now();
            LocalDateTime nextTime = nextSyncTime(schedule.getScheduleCron(), finishedAt);
            scheduleRepository.markSuccess(schedule.getScheduleId(), snapshot.getEtag(), snapshot.getLastModified(), snapshot.getContentHash(), finishedAt, nextTime);
            execRepository.markSuccess(exec.getExecId(), processLog.getProcessLogId(), snapshot.getEtag(), snapshot.getLastModified(), snapshot.getContentHash(), snapshot.getContentLength(), fetchDurationMs, System.currentTimeMillis() - syncStart);
            return execRepository.findByExecId(exec.getExecId()).orElseThrow();
        } catch (RuntimeException exception) {
            LocalDateTime finishedAt = LocalDateTime.now();
            LocalDateTime nextTime = nextSyncTime(schedule.getScheduleCron(), finishedAt);
            scheduleRepository.markFailed(schedule.getScheduleId(), resolveErrorCode(exception), shorten(exception.getMessage()), finishedAt, nextTime);
            execRepository.markFailed(exec.getExecId(), resolveErrorCode(exception), shorten(exception.getMessage()), null, System.currentTimeMillis() - syncStart);
            throw exception;
        }
    }

    /**
     * dispatchOrExecuteDueSchedule（投递或执行到期任务）用于定时扫描后按配置选择 MQ 或本机同步执行。
     */
    private void dispatchOrExecuteDueSchedule(KnowledgeDocumentScheduleEntity schedule, LocalDateTime now) {
        String leaseOwner = buildLeaseOwner();
        LocalDateTime leaseUntil = now.plusMinutes(properties.getLeaseMinutes());
        if (!requiredScheduleRepository().acquireLease(schedule.getScheduleId(), leaseOwner, leaseUntil, now)) {
            createSkippedExec(schedule, "同步任务正在被其他实例处理");
            return;
        }
        if (!properties.isDispatchToMq()) {
            executeLeasedSchedule(schedule.getScheduleId());
            return;
        }
        KnowledgeUrlSyncProducer producer = urlSyncProducerProvider.getIfAvailable();
        if (producer == null) {
            requiredScheduleRepository().releaseLease(schedule.getScheduleId());
            throw new BusinessException(ErrorCode.ASYNC_PROCESS_UNAVAILABLE, "URL 同步已配置投递 MQ，但当前没有可用的 KnowledgeUrlSyncProducer");
        }
        try {
            producer.send(KnowledgeUrlSyncMessage.builder()
                    .messageId(newId("knowledge_url_sync_msg"))
                    .scheduleId(schedule.getScheduleId())
                    .documentId(schedule.getDocumentId())
                    .knowledgeBaseId(schedule.getKnowledgeBaseId())
                    .triggerType(KnowledgeDocumentProcessTriggerType.SCHEDULE)
                    .retryTimes(0)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (RuntimeException exception) {
            requiredScheduleRepository().releaseLease(schedule.getScheduleId());
            throw exception;
        }
    }

    /**
     * createRunningExec（创建运行中执行记录）用于记录一次 URL 同步开始。
     */
    private KnowledgeDocumentScheduleExecEntity createRunningExec(KnowledgeDocumentScheduleEntity schedule) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocumentScheduleExecEntity entity = KnowledgeDocumentScheduleExecEntity.builder()
                .id(newId("knowledge_document_schedule_exec_pk"))
                .execId(newId("knowledge_document_schedule_exec"))
                .scheduleId(schedule.getScheduleId())
                .documentId(schedule.getDocumentId())
                .knowledgeBaseId(schedule.getKnowledgeBaseId())
                .processLogId(null)
                .triggerType(KnowledgeDocumentProcessTriggerType.SCHEDULE.value())
                .status(KnowledgeDocumentScheduleExecStatus.RUNNING.value())
                .sourceLocation(schedule.getSourceLocation())
                .previousEtag(schedule.getLastEtag())
                .previousModified(schedule.getLastModified())
                .previousContentHash(schedule.getLastContentHash())
                .startTime(now)
                .createTime(now)
                .deleted(0)
                .build();
        return requiredExecRepository().save(entity);
    }

    /**
     * createSkippedExec（创建跳过执行记录）用于记录租约冲突等未真正执行的情况。
     */
    private KnowledgeDocumentScheduleExecEntity createSkippedExec(KnowledgeDocumentScheduleEntity schedule, String message) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocumentScheduleExecEntity entity = KnowledgeDocumentScheduleExecEntity.builder()
                .id(newId("knowledge_document_schedule_exec_pk"))
                .execId(newId("knowledge_document_schedule_exec"))
                .scheduleId(schedule.getScheduleId())
                .documentId(schedule.getDocumentId())
                .knowledgeBaseId(schedule.getKnowledgeBaseId())
                .triggerType(KnowledgeDocumentProcessTriggerType.SCHEDULE.value())
                .status(KnowledgeDocumentScheduleExecStatus.SKIPPED.value())
                .sourceLocation(schedule.getSourceLocation())
                .previousEtag(schedule.getLastEtag())
                .previousModified(schedule.getLastModified())
                .previousContentHash(schedule.getLastContentHash())
                .errorMessage(message)
                .startTime(now)
                .endTime(now)
                .createTime(now)
                .deleted(0)
                .build();
        return requiredExecRepository().save(entity);
    }

    private KnowledgeDocumentScheduleEntity requiredSchedule(String scheduleId) {
        return requiredScheduleRepository().findByScheduleId(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到 URL 同步任务，scheduleId：" + scheduleId));
    }

    private KnowledgeDocumentEntity requiredDocument(String documentId) {
        return requiredKnowledgeDocumentRepository().findByDocumentId(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档，documentId：" + documentId));
    }

    private KnowledgeDocumentRepository requiredKnowledgeDocumentRepository() {
        KnowledgeDocumentRepository repository = knowledgeDocumentRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "URL 同步需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private KnowledgeDocumentScheduleRepository requiredScheduleRepository() {
        KnowledgeDocumentScheduleRepository repository = scheduleRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "URL 同步需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private KnowledgeDocumentScheduleExecRepository requiredExecRepository() {
        KnowledgeDocumentScheduleExecRepository repository = execRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "URL 同步需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private LocalDateTime nextSyncTime(String cron, LocalDateTime baseTime) {
        if (cron == null || cron.isBlank()) {
            return baseTime.plusHours(6);
        }
        LocalDateTime nextTime = CronExpression.parse(cron).next(baseTime);
        return nextTime == null ? baseTime.plusHours(6) : nextTime;
    }

    private String buildLeaseOwner() {
        return "clipforge-url-sync-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveErrorCode(RuntimeException exception) {
        if (exception instanceof BusinessException businessException) {
            return businessException.getErrorCode().getCode();
        }
        return ErrorCode.INTERNAL_ERROR.getCode();
    }

    private String shorten(String value) {
        if (value == null || value.isBlank()) {
            return "URL sync failed";
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private KnowledgeUrlSyncScheduleResponse toScheduleResponse(KnowledgeDocumentScheduleEntity entity) {
        return KnowledgeUrlSyncScheduleResponse.builder()
                .scheduleId(entity.getScheduleId())
                .documentId(entity.getDocumentId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .sourceLocation(entity.getSourceLocation())
                .scheduleCron(entity.getScheduleCron())
                .enabled(entity.getEnabled())
                .lastEtag(entity.getLastEtag())
                .lastModified(entity.getLastModified())
                .lastContentHash(entity.getLastContentHash())
                .lastSyncTime(entity.getLastSyncTime())
                .nextSyncTime(entity.getNextSyncTime())
                .leaseOwner(entity.getLeaseOwner())
                .leaseUntil(entity.getLeaseUntil())
                .lastExecStatus(entity.getLastExecStatus())
                .lastErrorCode(entity.getLastErrorCode())
                .lastErrorMessage(entity.getLastErrorMessage())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    private KnowledgeUrlSyncExecutionResponse toExecutionResponse(KnowledgeDocumentScheduleExecEntity entity) {
        return KnowledgeUrlSyncExecutionResponse.builder()
                .execId(entity.getExecId())
                .scheduleId(entity.getScheduleId())
                .documentId(entity.getDocumentId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .processLogId(entity.getProcessLogId())
                .triggerType(entity.getTriggerType())
                .status(entity.getStatus())
                .sourceLocation(entity.getSourceLocation())
                .previousEtag(entity.getPreviousEtag())
                .previousModified(entity.getPreviousModified())
                .previousContentHash(entity.getPreviousContentHash())
                .currentEtag(entity.getCurrentEtag())
                .currentModified(entity.getCurrentModified())
                .currentContentHash(entity.getCurrentContentHash())
                .contentLength(entity.getContentLength())
                .fetchDurationMs(entity.getFetchDurationMs())
                .syncDurationMs(entity.getSyncDurationMs())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .build();
    }
}
