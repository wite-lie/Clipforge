package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.CreationHistoryMemoryEntity;
import java.util.List;
import java.util.Optional;

/**
 * CreationHistoryMemoryRepository（创作历史记忆仓储）。
 *
 * <p>该接口负责管理历史高质量创作方案摘要。
 */
public interface CreationHistoryMemoryRepository {

    /**
     * save（保存）用于新增创作历史记忆。
     *
     * @param entity 创作历史记忆实体
     * @return 已保存实体
     */
    CreationHistoryMemoryEntity save(CreationHistoryMemoryEntity entity);

    /**
     * findByMemoryId（按记忆编号查询）用于查询单条历史记忆。
     *
     * @param memoryId 记忆编号
     * @return 可能存在的历史记忆
     */
    Optional<CreationHistoryMemoryEntity> findByMemoryId(String memoryId);

    /**
     * listRecentActiveByUserId（按用户查询近期历史记忆）用于兜底读取用户历史经验。
     *
     * @param userId 用户编号
     * @param limit 最大返回数量
     * @return 历史记忆列表
     */
    List<CreationHistoryMemoryEntity> listRecentActiveByUserId(String userId, int limit);

    /**
     * listActiveByProjectId（按项目查询启用历史记忆）用于项目记忆管理接口。
     *
     * @param projectId 项目编号
     * @param platform 目标平台，可以为空
     * @param limit 最大返回数量
     * @return 历史记忆列表
     */
    List<CreationHistoryMemoryEntity> listActiveByProjectId(String projectId, String platform, int limit);

    /**
     * listHighScoreByProjectAndPlatform（按项目和平台查询高分历史记忆）用于创作前注入经验。
     *
     * @param projectId 项目编号
     * @param platform 目标平台
     * @param minScore 最低评分
     * @param limit 最大返回数量
     * @return 高分历史记忆列表
     */
    List<CreationHistoryMemoryEntity> listHighScoreByProjectAndPlatform(String projectId, String platform, int minScore, int limit);

    /**
     * disableByMemoryId（按记忆编号停用）用于人工修正历史记忆。
     *
     * @param memoryId 记忆编号
     * @return 是否更新成功
     */
    boolean disableByMemoryId(String memoryId);
}
