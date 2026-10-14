package com.clipforge.application;

import com.clipforge.contract.knowledge.AsyncProcessKnowledgeDocumentResponse;
import com.clipforge.knowledge.application.api.KnowledgeDocumentAsyncProcessUseCase;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentProcessLogEntity;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessLogStatus;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessPhase;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessTriggerType;
import com.clipforge.knowledge.application.processing.KnowledgeDocumentStatusUseCase;
import com.clipforge.knowledge.application.processing.KnowledgeDocumentProcessLogUseCase;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentRepository;
import com.clipforge.messaging.rocketmq.rag.document.KnowledgeDocumentProcessMessage;
import com.clipforge.messaging.rocketmq.rag.document.KnowledgeDocumentProcessProducer;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatus;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatusCache;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * KnowledgeDocumentAsyncProcessApplicationService（知识文档异步处理应用服务）。
 *
 * <p>该服务负责把“处理知识文档”的慢操作投递到 RocketMQ（消息队列），接口侧快速返回 processLogId（处理日志编号），后续通过状态查询接口观察进度。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentAsyncProcessApplicationService implements KnowledgeDocumentAsyncProcessUseCase {

    /** knowledgeDocumentRepositoryProvider（知识文档仓储提供器）用于校验文档和更新文档状态。 */
    private final ObjectProvider<KnowledgeDocumentRepository> knowledgeDocumentRepositoryProvider;

    /** knowledgeDocumentStatusUseCaseProvider（知识文档状态用例提供器）用于更新文档处理状态。 */
    private final ObjectProvider<KnowledgeDocumentStatusUseCase> knowledgeDocumentStatusUseCaseProvider;

    /** processLogUseCaseProvider（处理日志用例提供器）用于兼容未启用 db Profile 的环境。 */
    private final ObjectProvider<KnowledgeDocumentProcessLogUseCase> processLogUseCaseProvider;

    /** processProducerProvider（处理消息生产者提供器）用于可选获取 RocketMQ（消息队列）生产者实现。 */
    private final ObjectProvider<KnowledgeDocumentProcessProducer> processProducerProvider;

    /** statusCacheProvider（状态缓存提供器）用于把异步任务初始状态写入 Redis（缓存）。 */
    private final ObjectProvider<KnowledgeDocumentProcessStatusCache> statusCacheProvider;

    /**
     * startAsyncProcess（启动异步处理）用于提交知识文档异步处理消息。
     *
     * @param documentId 知识文档编号
     * @return 异步处理响应
     */
    public AsyncProcessKnowledgeDocumentResponse startAsyncProcess(String documentId) {
        // 查询知识文档，确保文档存在。
        KnowledgeDocumentEntity document = requiredDocument(documentId);
        // 先确认 RocketMQ（消息队列）生产者可用，避免未启用 MQ 时污染文档状态。
        KnowledgeDocumentProcessProducer producer = requiredProducer();
        // 尝试把文档标记为 PROCESSING（处理中），防止重复提交。
        boolean marked = requiredKnowledgeDocumentStatusUseCase().tryMarkProcessing(documentId);
        // 如果没有标记成功，说明文档不存在或已经处于处理中。
        if (!marked) {
            // 抛出明确错误，提示调用方查询现有处理状态。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识文档正在处理中，请通过状态查询接口观察进度，documentId：" + documentId);
        }
        // 创建处理日志，异步消费者后续会复用该日志。
        KnowledgeDocumentProcessLogEntity processLog = requiredProcessLogUseCase().createRunningProcessLog(
                document,
                KnowledgeDocumentProcessTriggerType.MANUAL);
        // 构造消息。
        KnowledgeDocumentProcessMessage message = buildMessage(document, processLog);
        try {
            // 写入 Redis（缓存）初始状态。
            saveRunningStatus(message);
            // 投递 RocketMQ（消息队列）消息。
            producer.send(message);
            // 返回异步提交结果。
            return AsyncProcessKnowledgeDocumentResponse.builder()
                    // 设置文档编号。
                    .documentId(document.getDocumentId())
                    // 设置知识库编号。
                    .knowledgeBaseId(document.getKnowledgeBaseId())
                    // 设置处理日志编号。
                    .processLogId(processLog.getProcessLogId())
                    // 设置消息编号。
                    .messageId(message.getMessageId())
                    // 设置初始状态。
                    .status(KnowledgeDocumentProcessLogStatus.RUNNING.value())
                    // 设置初始阶段。
                    .phase(KnowledgeDocumentProcessPhase.PARSE_CHUNK.value())
                    // 设置响应消息。
                    .message("知识文档异步处理任务已提交")
                    // 设置接收时间。
                    .acceptedAt(message.getCreatedAt())
                    // 设置状态查询地址。
                    .statusUrl("/api/knowledge-document-process-logs/" + processLog.getProcessLogId() + "/status")
                    // 构建响应。
                    .build();
        } catch (RuntimeException exception) {
            // 发送失败时标记处理日志失败。
            markProcessLogFailedQuietly(processLog.getProcessLogId(), exception);
            // 发送失败时标记文档失败，避免文档长期停留在 PROCESSING（处理中）。
            markDocumentFailedQuietly(documentId, exception);
            // 写入 Redis（缓存）失败状态。
            saveFailedStatus(message, exception);
            // 抛出业务异常。
            throw new BusinessException(ErrorCode.ASYNC_PROCESS_UNAVAILABLE, "RocketMQ（消息队列）异步处理消息发送失败：" + exception.getMessage());
        }
    }

    /**
     * buildMessage（构造消息）用于把文档和处理日志转换为 RocketMQ（消息队列）业务消息。
     *
     * @param document 知识文档实体
     * @param processLog 处理日志实体
     * @return 知识文档处理消息
     */
    private KnowledgeDocumentProcessMessage buildMessage(KnowledgeDocumentEntity document, KnowledgeDocumentProcessLogEntity processLog) {
        // 使用 Builder（建造者模式）构造消息。
        return KnowledgeDocumentProcessMessage.builder()
                // 设置消息编号。
                .messageId("kdoc_proc_msg_" + shortUuid())
                // 设置知识文档编号。
                .documentId(document.getDocumentId())
                // 设置知识库编号。
                .knowledgeBaseId(document.getKnowledgeBaseId())
                // 设置处理日志编号。
                .processLogId(processLog.getProcessLogId())
                // 设置触发类型。
                .triggerType(KnowledgeDocumentProcessTriggerType.MANUAL.value())
                // 首次投递重试次数为 0。
                .retryTimes(0)
                // 设置创建时间。
                .createdAt(LocalDateTime.now())
                // 构建消息。
                .build();
    }

    /**
     * saveRunningStatus（保存运行中状态）用于异步任务提交成功前后让状态查询接口立即可见。
     *
     * @param message 处理消息
     */
    private void saveRunningStatus(KnowledgeDocumentProcessMessage message) {
        // 获取状态缓存实现。
        KnowledgeDocumentProcessStatusCache statusCache = statusCacheProvider.getIfAvailable();
        // 如果未启用 Redis（缓存），则直接跳过。
        if (statusCache == null) {
            // 直接返回。
            return;
        }
        try {
            // 写入运行中状态。
            statusCache.save(KnowledgeDocumentProcessStatus.running(
                    message.getDocumentId(),
                    message.getKnowledgeBaseId(),
                    message.getProcessLogId(),
                    message.getTriggerType()));
        } catch (RuntimeException ignored) {
            // Redis（缓存）状态写入失败不能阻止 RocketMQ（消息队列）任务投递。
        }
    }

    /**
     * saveFailedStatus（保存失败状态）用于消息发送失败时写入 Redis（缓存）。
     *
     * @param message 处理消息
     * @param exception 发送异常
     */
    private void saveFailedStatus(KnowledgeDocumentProcessMessage message, RuntimeException exception) {
        // 获取状态缓存实现。
        KnowledgeDocumentProcessStatusCache statusCache = statusCacheProvider.getIfAvailable();
        // 如果未启用 Redis（缓存），则直接跳过。
        if (statusCache == null) {
            // 直接返回。
            return;
        }
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        try {
            // 写入失败状态。
            statusCache.save(KnowledgeDocumentProcessStatus.builder()
                    // 设置文档编号。
                    .documentId(message.getDocumentId())
                    // 设置知识库编号。
                    .knowledgeBaseId(message.getKnowledgeBaseId())
                    // 设置处理日志编号。
                    .processLogId(message.getProcessLogId())
                    // 设置失败状态。
                    .status(KnowledgeDocumentProcessLogStatus.FAILED)
                    // 发送阶段尚未进入消费端，因此仍归到解析切片前置阶段。
                    .phase(KnowledgeDocumentProcessPhase.PARSE_CHUNK)
                    // 设置触发类型。
                    .triggerType(parseTriggerType(message.getTriggerType()))
                    // 失败时进度视为结束。
                    .progressPercent(100)
                    // 设置消息。
                    .message("知识文档异步处理消息发送失败")
                    // 设置错误码。
                    .errorCode(ErrorCode.ASYNC_PROCESS_UNAVAILABLE.getCode())
                    // 设置错误消息。
                    .errorMessage(exception.getMessage())
                    // 设置开始时间。
                    .startedAt(message.getCreatedAt())
                    // 设置更新时间。
                    .updatedAt(now)
                    // 设置结束时间。
                    .endedAt(now)
                    // 构建状态。
                    .build());
        } catch (RuntimeException ignored) {
            // Redis（缓存）失败状态写入失败不能覆盖 RocketMQ（消息队列）发送失败这个主异常。
        }
    }

    /**
     * markProcessLogFailedQuietly（静默标记处理日志失败）用于避免日志更新失败覆盖消息发送异常。
     *
     * @param processLogId 处理日志编号
     * @param exception 发送异常
     */
    private void markProcessLogFailedQuietly(String processLogId, RuntimeException exception) {
        try {
            // 标记处理日志失败。
            requiredProcessLogUseCase().markFailed(processLogId, KnowledgeDocumentProcessPhase.PARSE_CHUNK.value(), null, null, 0L, ErrorCode.ASYNC_PROCESS_UNAVAILABLE.getCode(), exception.getMessage());
        } catch (RuntimeException ignored) {
            // 日志失败不能覆盖主异常。
        }
    }

    /**
     * markDocumentFailedQuietly（静默标记文档失败）用于消息发送失败时恢复文档状态。
     *
     * @param documentId 知识文档编号
     * @param exception 发送异常
     */
    private void markDocumentFailedQuietly(String documentId, RuntimeException exception) {
        try {
            // 标记文档失败。
            requiredKnowledgeDocumentStatusUseCase().markFailed(documentId, "RocketMQ（消息队列）异步处理消息发送失败：" + exception.getMessage());
        } catch (RuntimeException ignored) {
            // 文档状态更新失败不能覆盖主异常。
        }
    }

    /**
     * requiredDocument（获取必需文档）用于校验知识文档存在。
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
     * requiredProducer（获取必需生产者）用于确保当前环境启用了 RocketMQ（消息队列）异步处理能力。
     *
     * @return 知识文档处理消息生产者
     */
    private KnowledgeDocumentProcessProducer requiredProducer() {
        // 从 Spring（Spring 框架）容器中获取生产者。
        KnowledgeDocumentProcessProducer producer = processProducerProvider.getIfAvailable();
        // 如果生产者不存在，说明没有启用 rocketmq Profile（消息队列配置环境）或异步开关。
        if (producer == null) {
            // 抛出异步能力不可用异常。
            throw new BusinessException(ErrorCode.ASYNC_PROCESS_UNAVAILABLE, "知识文档异步处理需要启用 rocketmq Profile（消息队列配置环境）");
        }
        // 返回生产者。
        return producer;
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
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档异步处理需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return repository;
    }

    /**
     * requiredKnowledgeDocumentStatusUseCase（获取必需知识文档状态用例）用于更新文档处理状态。
     *
     * @return 知识文档状态用例
     */
    private KnowledgeDocumentStatusUseCase requiredKnowledgeDocumentStatusUseCase() {
        // 从 Spring（Spring 框架）容器中获取状态用例。
        KnowledgeDocumentStatusUseCase useCase = knowledgeDocumentStatusUseCaseProvider.getIfAvailable();
        // 如果状态用例不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (useCase == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档状态更新需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用状态用例。
        return useCase;
    }

    /**
     * requiredProcessLogUseCase（获取必需处理日志用例）用于在数据库环境下写入处理日志。
     *
     * @return 处理日志用例
     */
    private KnowledgeDocumentProcessLogUseCase requiredProcessLogUseCase() {
        KnowledgeDocumentProcessLogUseCase useCase = processLogUseCaseProvider.getIfAvailable();
        if (useCase == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档异步处理日志需要启用 db Profile（数据库配置环境）");
        }
        return useCase;
    }

    /**
     * shortUuid（短随机编号）用于生成较短的业务消息编号。
     *
     * @return 无横线 UUID（通用唯一识别码）字符串
     */
    private String shortUuid() {
        // 返回去除横线后的 UUID（通用唯一识别码）。
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * parseTriggerType（解析触发类型）用于把消息契约中的字符串恢复为状态对象里的业务枚举。
     */
    private KnowledgeDocumentProcessTriggerType parseTriggerType(String triggerType) {
        if (triggerType == null || triggerType.isBlank()) {
            return KnowledgeDocumentProcessTriggerType.MANUAL;
        }
        try {
            return KnowledgeDocumentProcessTriggerType.valueOf(triggerType);
        } catch (IllegalArgumentException ignored) {
            return KnowledgeDocumentProcessTriggerType.MANUAL;
        }
    }
}
