package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.KnowledgeDocumentScheduleEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * KnowledgeDocumentScheduleRepository（知识文档定时任务仓储）。
 *
 * <p>该接口负责管理 URL（统一资源定位符）定时同步任务的查询、租约和结果回写。
 */
public interface KnowledgeDocumentScheduleRepository {

    /**
     * save（保存）用于新增定时任务。
     *
     * @param entity 定时任务实体
     * @return 已保存实体
     */
    KnowledgeDocumentScheduleEntity save(KnowledgeDocumentScheduleEntity entity);

    /**
     * updateDefinition（更新任务定义）用于修改 Cron（定时表达式）和启用状态。
     *
     * @param scheduleId 定时任务编号
     * @param sourceLocation 原始 URL 地址
     * @param scheduleCron Cron（定时表达式）
     * @param enabled 是否启用
     * @param nextSyncTime 下一次同步时间
     * @return 更新后的实体
     */
    KnowledgeDocumentScheduleEntity updateDefinition(String scheduleId, String sourceLocation, String scheduleCron, Integer enabled, LocalDateTime nextSyncTime);

    /**
     * findByScheduleId（按任务编号查询）用于查询单个定时任务。
     *
     * @param scheduleId 定时任务编号
     * @return 可能存在的定时任务
     */
    Optional<KnowledgeDocumentScheduleEntity> findByScheduleId(String scheduleId);

    /**
     * findByDocumentId（按文档编号查询）用于按文档反查定时任务。
     *
     * @param documentId 知识文档编号
     * @return 可能存在的定时任务
     */
    Optional<KnowledgeDocumentScheduleEntity> findByDocumentId(String documentId);

    /**
     * listByKnowledgeBaseId（按知识库查询）用于读取某个知识库下的 URL 同步任务。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 定时任务列表
     */
    List<KnowledgeDocumentScheduleEntity> listByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * listDueSchedules（查询到期任务）用于扫描当前时刻需要执行的任务。
     *
     * @param now 当前时间
     * @param limit 最大返回数量
     * @return 到期任务列表
     */
    List<KnowledgeDocumentScheduleEntity> listDueSchedules(LocalDateTime now, int limit);

    /**
     * acquireLease（获取租约）用于防止重复调度。
     *
     * @param scheduleId 定时任务编号
     * @param leaseOwner 租约持有者
     * @param leaseUntil 租约到期时间
     * @param now 当前时间
     * @return 是否获取成功
     */
    boolean acquireLease(String scheduleId, String leaseOwner, LocalDateTime leaseUntil, LocalDateTime now);

    /**
     * releaseLease（释放租约）用于结束任务后清理锁信息。
     *
     * @param scheduleId 定时任务编号
     * @return 更新后的实体
     */
    KnowledgeDocumentScheduleEntity releaseLease(String scheduleId);

    /**
     * markSuccess（标记成功）用于记录本次同步成功并刷新下一次同步时间。
     *
     * @param scheduleId 定时任务编号
     * @param lastEtag 最近 ETag
     * @param lastModified 最近修改时间
     * @param lastContentHash 最近内容哈希
     * @param lastSyncTime 最近同步时间
     * @param nextSyncTime 下一次同步时间
     * @return 更新后的实体
     */
    KnowledgeDocumentScheduleEntity markSuccess(String scheduleId, String lastEtag, String lastModified, String lastContentHash, LocalDateTime lastSyncTime, LocalDateTime nextSyncTime);

    /**
     * markSkipped（标记跳过）用于记录内容未变化的同步结果。
     *
     * @param scheduleId 定时任务编号
     * @param lastSyncTime 最近同步时间
     * @param nextSyncTime 下一次同步时间
     * @return 更新后的实体
     */
    KnowledgeDocumentScheduleEntity markSkipped(String scheduleId, LocalDateTime lastSyncTime, LocalDateTime nextSyncTime);

    /**
     * markFailed（标记失败）用于记录同步失败。
     *
     * @param scheduleId 定时任务编号
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @param lastSyncTime 最近同步时间
     * @param nextSyncTime 下一次同步时间
     * @return 更新后的实体
     */
    KnowledgeDocumentScheduleEntity markFailed(String scheduleId, String errorCode, String errorMessage, LocalDateTime lastSyncTime, LocalDateTime nextSyncTime);
}
