package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.KnowledgeDocumentProcessLogEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * KnowledgeDocumentProcessLogRepository（知识文档处理日志仓储）。
 *
 * <p>该接口负责记录知识文档处理链路，应用层只依赖该抽象，避免直接依赖数据库实现细节。
 */
public interface KnowledgeDocumentProcessLogRepository {

    /**
     * save（保存）用于创建处理日志。
     *
     * @param entity 处理日志实体
     * @return 已保存处理日志实体
     */
    KnowledgeDocumentProcessLogEntity save(KnowledgeDocumentProcessLogEntity entity);

    /**
     * findByProcessLogId（按处理日志编号查询）用于处理完成后读取最新日志。
     *
     * @param processLogId 处理日志编号
     * @return 可能存在的处理日志实体
     */
    Optional<KnowledgeDocumentProcessLogEntity> findByProcessLogId(String processLogId);

    /**
     * findLatestByDocumentId（按文档编号查询最近处理日志）用于状态查询接口在 Redis（缓存）未命中时回查数据库。
     *
     * @param documentId 知识文档编号
     * @return 可能存在的最近处理日志实体
     */
    Optional<KnowledgeDocumentProcessLogEntity> findLatestByDocumentId(String documentId);

    /**
     * listByDocumentId（按文档编号查询日志）用于管理接口展示某个文档的全部处理历史。
     *
     * @param documentId 知识文档编号
     * @return 处理日志列表
     */
    List<KnowledgeDocumentProcessLogEntity> listByDocumentId(String documentId);

    /**
     * listRunningBefore（查询超时运行中日志）用于扫描长时间未结束的知识文档处理任务。
     *
     * @param beforeTime 截止时间，开始时间早于该时间且仍在运行中的日志会被认为可能卡住
     * @param limit 查询上限
     * @return 运行中处理日志列表
     */
    List<KnowledgeDocumentProcessLogEntity> listRunningBefore(LocalDateTime beforeTime, int limit);

    /**
     * countByDocumentIdAndTriggerType（按文档和触发类型计数）用于统计人工或消息重试次数。
     *
     * @param documentId 知识文档编号
     * @param triggerType 触发类型
     * @return 日志数量
     */
    long countByDocumentIdAndTriggerType(String documentId, String triggerType);

    /**
     * markSuccess（标记成功）用于完整处理成功后更新耗时、切片数量和最终阶段。
     *
     * @param processLogId 处理日志编号
     * @param phase 最终阶段
     * @param chunkDurationMs 解析切片耗时毫秒
     * @param embeddingDurationMs 向量化耗时毫秒
     * @param totalDurationMs 总耗时毫秒
     * @param chunkCount 切片数量
     * @return 更新后的处理日志实体
     */
    KnowledgeDocumentProcessLogEntity markSuccess(String processLogId, String phase, Long chunkDurationMs, Long embeddingDurationMs, Long totalDurationMs, Integer chunkCount);

    /**
     * markFailed（标记失败）用于完整处理失败后记录失败阶段和错误原因。
     *
     * @param processLogId 处理日志编号
     * @param phase 失败阶段
     * @param chunkDurationMs 已完成的解析切片耗时毫秒
     * @param embeddingDurationMs 已完成的向量化耗时毫秒
     * @param totalDurationMs 总耗时毫秒
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @return 更新后的处理日志实体
     */
    KnowledgeDocumentProcessLogEntity markFailed(String processLogId, String phase, Long chunkDurationMs, Long embeddingDurationMs, Long totalDurationMs, String errorCode, String errorMessage);
}
