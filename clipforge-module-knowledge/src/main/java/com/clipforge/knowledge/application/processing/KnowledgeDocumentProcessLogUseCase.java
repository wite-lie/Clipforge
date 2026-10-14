package com.clipforge.knowledge.application.processing;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentProcessLogEntity;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessTriggerType;

/**
 * KnowledgeDocumentProcessLogUseCase（知识文档处理日志用例）。
 *
 * <p>该端口统一处理日志的创建、成功和失败写入，隔离应用编排对具体日志仓储的直接依赖。
 * 日志查询仍由需要恢复判断和状态查询的应用服务通过查询仓储完成。
 */
public interface KnowledgeDocumentProcessLogUseCase {

    /**
     * createRunningProcessLog（创建运行中处理日志）用于处理开始前创建一条运行日志。
     *
     * @param document 目标文档
     * @param triggerType 触发类型
     * @return 已保存的处理日志
     */
    KnowledgeDocumentProcessLogEntity createRunningProcessLog(
            KnowledgeDocumentEntity document,
            KnowledgeDocumentProcessTriggerType triggerType);

    /**
     * markSuccess（标记成功）用于处理链路成功后写入阶段、耗时和切片数量。
     *
     * @param processLogId 处理日志编号
     * @param phase 最终处理阶段
     * @param chunkDurationMs 切片阶段耗时
     * @param embeddingDurationMs 向量化阶段耗时
     * @param totalDurationMs 总耗时
     * @param chunkCount 切片数量
     * @return 更新后的处理日志
     */
    KnowledgeDocumentProcessLogEntity markSuccess(
            String processLogId,
            String phase,
            Long chunkDurationMs,
            Long embeddingDurationMs,
            Long totalDurationMs,
            Integer chunkCount);

    /**
     * markFailed（标记失败）用于处理链路失败后写入阶段和错误信息。
     *
     * @param processLogId 处理日志编号
     * @param phase 失败阶段
     * @param chunkDurationMs 已完成的切片耗时
     * @param embeddingDurationMs 已完成的向量化耗时
     * @param totalDurationMs 总耗时
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @return 更新后的处理日志
     */
    KnowledgeDocumentProcessLogEntity markFailed(
            String processLogId,
            String phase,
            Long chunkDurationMs,
            Long embeddingDurationMs,
            Long totalDurationMs,
            String errorCode,
            String errorMessage);
}
