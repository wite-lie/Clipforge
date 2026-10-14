package com.clipforge.memory.infrastructure.persistence.repository;

import com.clipforge.memory.infrastructure.persistence.entity.ProjectMemoryEntity;
import java.util.Optional;

/**
 * ProjectMemoryRepository（项目记忆仓储）。
 *
 * <p>该接口负责管理项目级 Memory（记忆）的保存、读取和停用。
 */
public interface ProjectMemoryRepository {

    /**
     * save（保存）用于新增项目记忆。
     *
     * @param entity 项目记忆实体
     * @return 已保存实体
     */
    ProjectMemoryEntity save(ProjectMemoryEntity entity);

    /**
     * findByMemoryId（按记忆编号查询）用于查询单条项目记忆。
     *
     * @param memoryId 记忆编号
     * @return 可能存在的项目记忆
     */
    Optional<ProjectMemoryEntity> findByMemoryId(String memoryId);

    /**
     * findActiveByProjectId（按项目查询启用记忆）用于读取项目画像。
     *
     * @param projectId 项目编号
     * @return 可能存在的项目记忆
     */
    Optional<ProjectMemoryEntity> findActiveByProjectId(String projectId);

    /**
     * disableByMemoryId（按记忆编号停用）用于人工修正项目记忆。
     *
     * @param memoryId 记忆编号
     * @return 是否更新成功
     */
    boolean disableByMemoryId(String memoryId);
}
