package com.clipforge.knowledge.infrastructure.persistence.repository;

import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentScheduleExecEntity;
import java.util.List;
import java.util.Optional;

/**
 * KnowledgeDocumentScheduleExecRepository（知识文档定时执行记录仓储）。
 *
 * <p>该接口负责管理 URL（统一资源定位符）定时同步的执行日志。
 */
public interface KnowledgeDocumentScheduleExecRepository {

    /**
     * save（保存）用于新增执行记录。
     *
     * @param entity 执行记录实体
     * @return 已保存实体
     */
    KnowledgeDocumentScheduleExecEntity save(KnowledgeDocumentScheduleExecEntity entity);

    /**
     * findByExecId（按执行编号查询）用于回查单条执行记录。
     *
     * @param execId 执行编号
     * @return 可能存在的执行记录
     */
    Optional<KnowledgeDocumentScheduleExecEntity> findByExecId(String execId);

    /**
     * listByScheduleId（按任务编号查询）用于读取某个 URL 同步任务的执行历史。
     *
     * @param scheduleId 定时任务编号
     * @param limit 最大返回数量
     * @return 执行历史列表
     */
    List<KnowledgeDocumentScheduleExecEntity> listByScheduleId(String scheduleId, int limit);

    /**
     * markSuccess（标记成功）用于更新执行结果。
     *
     * @param execId 执行编号
     * @param processLogId 处理日志编号
     * @param currentEtag 当前 ETag
     * @param currentModified 当前 Last-Modified
     * @param currentContentHash 当前内容哈希
     * @param contentLength 内容长度
     * @param fetchDurationMs 抓取耗时
     * @param syncDurationMs 同步总耗时
     * @return 更新后的实体
     */
    KnowledgeDocumentScheduleExecEntity markSuccess(String execId, String processLogId, String currentEtag, String currentModified, String currentContentHash, Long contentLength, Long fetchDurationMs, Long syncDurationMs);

    /**
     * markSkipped（标记跳过）用于更新执行结果。
     *
     * @param execId 执行编号
     * @param currentEtag 当前 ETag
     * @param currentModified 当前 Last-Modified
     * @param currentContentHash 当前内容哈希
     * @param contentLength 内容长度
     * @param fetchDurationMs 抓取耗时
     * @param syncDurationMs 同步总耗时
     * @param errorMessage 跳过原因
     * @return 更新后的实体
     */
    KnowledgeDocumentScheduleExecEntity markSkipped(String execId, String currentEtag, String currentModified, String currentContentHash, Long contentLength, Long fetchDurationMs, Long syncDurationMs, String errorMessage);

    /**
     * markFailed（标记失败）用于更新执行结果。
     *
     * @param execId 执行编号
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @param fetchDurationMs 抓取耗时
     * @param syncDurationMs 同步总耗时
     * @return 更新后的实体
     */
    KnowledgeDocumentScheduleExecEntity markFailed(String execId, String errorCode, String errorMessage, Long fetchDurationMs, Long syncDurationMs);
}
