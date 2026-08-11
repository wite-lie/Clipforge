package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.ProjectEntity;
import java.util.List;
import java.util.Optional;

/**
 * ProjectRepository（项目仓储）。
 *
 * <p>该接口负责保存和查询 Project（项目）。
 */
public interface ProjectRepository {

    /**
     * create（创建）用于创建一个长期创作项目。
     *
     * @param userId 用户编号
     * @param name 项目名称
     * @param description 项目描述
     * @return 已保存项目实体
     */
    ProjectEntity create(String userId, String name, String description);

    /**
     * findByProjectId（按项目编号查询）用于读取项目详情。
     *
     * @param projectId 项目编号
     * @return 可能存在的项目实体
     */
    Optional<ProjectEntity> findByProjectId(String projectId);

    /**
     * listByUserId（按用户编号查询列表）用于读取某个用户的项目列表。
     *
     * @param userId 用户编号
     * @return 项目实体列表
     */
    List<ProjectEntity> listByUserId(String userId);
}
