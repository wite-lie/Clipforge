package com.clipforge.rag.process;

import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessLogStatus;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessPhase;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessTriggerType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeDocumentProcessStatus（知识文档处理状态）。
 *
 * <p>该对象用于描述 Redis（缓存）和状态查询接口中的知识文档处理进度，后续异步任务会持续刷新该对象。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentProcessStatus {

    /** documentId（知识文档编号）表示当前处理状态对应的文档。 */
    private String documentId;

    /** knowledgeBaseId（知识库编号）表示文档所属知识库。 */
    private String knowledgeBaseId;

    /** processLogId（处理日志编号）用于关联数据库处理日志。 */
    private String processLogId;

    /** status（处理状态）表示 RUNNING（运行中）、SUCCESS（成功）、FAILED（失败）或 SKIPPED（跳过）。 */
    private KnowledgeDocumentProcessLogStatus status;

    /** phase（处理阶段）表示 PARSE_CHUNK（解析切片）、EMBEDDING（嵌入）或 READY（可检索）。 */
    private KnowledgeDocumentProcessPhase phase;

    /** triggerType（触发类型）表示 MANUAL（手动）、SCHEDULE（定时）、MQ_RETRY（消息重试）或 SYSTEM_SEED（系统种子）。 */
    private KnowledgeDocumentProcessTriggerType triggerType;

    /** progressPercent（进度百分比）用于给前端或测试日志展示粗粒度进度。 */
    private Integer progressPercent;

    /** message（状态消息）用于展示当前阶段的可读说明。 */
    private String message;

    /** errorCode（错误码）用于失败时保存机器可读错误编码。 */
    private String errorCode;

    /** errorMessage（错误消息）用于失败时保存人类可读失败原因。 */
    private String errorMessage;

    /** chunkCount（切片数量）表示已经生成或参与向量化的有效切片数量。 */
    private Integer chunkCount;

    /** chunkDurationMs（解析切片耗时毫秒）表示解析和切片阶段耗时。 */
    private Long chunkDurationMs;

    /** embeddingDurationMs（嵌入耗时毫秒）表示 Embedding（嵌入）和向量入库阶段耗时。 */
    private Long embeddingDurationMs;

    /** totalDurationMs（总耗时毫秒）表示完整处理链路耗时。 */
    private Long totalDurationMs;

    /** startedAt（开始时间）表示处理任务开始时刻。 */
    private LocalDateTime startedAt;

    /** updatedAt（更新时间）表示状态最近刷新时刻。 */
    private LocalDateTime updatedAt;

    /** endedAt（结束时间）表示处理任务结束时刻，运行中可以为空。 */
    private LocalDateTime endedAt;

    /**
     * running（运行中状态）用于创建异步任务刚启动时的状态对象。
     *
     * @param documentId 知识文档编号
     * @param knowledgeBaseId 知识库编号
     * @param processLogId 处理日志编号
     * @param triggerType 触发类型
     * @return 知识文档处理状态
     */
    public static KnowledgeDocumentProcessStatus running(String documentId, String knowledgeBaseId, String processLogId, String triggerType) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 构建运行中状态对象。
        return KnowledgeDocumentProcessStatus.builder()
                // 设置知识文档编号。
                .documentId(documentId)
                // 设置知识库编号。
                .knowledgeBaseId(knowledgeBaseId)
                // 设置处理日志编号。
                .processLogId(processLogId)
                // 设置处理状态为运行中。
                .status(KnowledgeDocumentProcessLogStatus.RUNNING)
                // 设置初始阶段为解析切片。
                .phase(KnowledgeDocumentProcessPhase.PARSE_CHUNK)
                // 设置触发类型。
                .triggerType(parseTriggerType(triggerType))
                // 设置初始进度。
                .progressPercent(10)
                // 设置状态消息。
                .message("知识文档异步处理已启动")
                // 设置开始时间。
                .startedAt(now)
                // 设置更新时间。
                .updatedAt(now)
                // 构建状态对象。
                .build();
    }

    /**
     * statusValue（处理状态值）用于获取可写入 Redis（缓存）或接口响应的状态字符串。
     *
     * @return 处理状态字符串
     */
    public String statusValue() {
        // 如果状态为空，则返回空引用。
        if (status == null) {
            // 返回空引用。
            return null;
        }
        // 返回枚举对应的数据库值。
        return status.value();
    }

    /**
     * phaseValue（处理阶段值）用于获取可写入 Redis（缓存）或接口响应的阶段字符串。
     *
     * @return 处理阶段字符串
     */
    public String phaseValue() {
        // 如果阶段为空，则返回空引用。
        if (phase == null) {
            // 返回空引用。
            return null;
        }
        // 返回枚举对应的数据库值。
        return phase.value();
    }

    /**
     * triggerTypeValue（触发类型值）用于获取可写入 Redis（缓存）或接口响应的触发类型字符串。
     *
     * @return 触发类型字符串
     */
    public String triggerTypeValue() {
        // 如果触发类型为空，则返回空引用。
        if (triggerType == null) {
            // 返回空引用。
            return null;
        }
        // 返回枚举对应的数据库值。
        return triggerType.value();
    }

    /**
     * parseTriggerType（解析触发类型）用于把消息契约中的字符串恢复为状态对象里的业务枚举。
     */
    private static KnowledgeDocumentProcessTriggerType parseTriggerType(String triggerType) {
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
