package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.infra.persistence.entity.ProjectEntity;
import com.clipforge.infra.persistence.mapper.ProjectMapper;
import com.clipforge.infra.persistence.repository.ProjectRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisProjectRepository（MyBatis 项目仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问项目表。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisProjectRepository implements ProjectRepository {

    /** projectMapper（项目映射器）负责执行项目表 CRUD（增删改查）。 */
    private final ProjectMapper projectMapper;

    /**
     * create（创建）用于创建一个长期创作项目。
     *
     * @param userId 用户编号
     * @param name 项目名称
     * @param description 项目描述
     * @return 已保存项目实体
     */
    @Override
    public ProjectEntity create(String userId, String name, String description) {
        // 获取当前时间，统一填充创建时间和更新时间。
        LocalDateTime now = LocalDateTime.now();
        // 使用 Builder（建造者模式）组装项目实体。
        ProjectEntity entity = ProjectEntity.builder()
                // 设置数据库内部主键。
                .id(newId("project_pk"))
                // 设置对外项目编号。
                .projectId(newId("project"))
                // 设置用户编号。
                .userId(userId)
                // 设置项目名称。
                .name(name)
                // 设置项目描述。
                .description(description)
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记为正常。
                .deleted(0)
                // 构建实体。
                .build();
        // 插入数据库。
        projectMapper.insert(entity);
        // 返回已保存项目实体。
        return entity;
    }

    /**
     * findByProjectId（按项目编号查询）用于读取项目详情。
     *
     * @param projectId 项目编号
     * @return 可能存在的项目实体
     */
    @Override
    public Optional<ProjectEntity> findByProjectId(String projectId) {
        // 创建按项目编号查询且排除逻辑删除数据的条件。
        LambdaQueryWrapper<ProjectEntity> queryWrapper = new LambdaQueryWrapper<ProjectEntity>()
                // projectId（项目编号）必须匹配。
                .eq(ProjectEntity::getProjectId, projectId)
                // deleted（逻辑删除）必须为正常。
                .eq(ProjectEntity::getDeleted, 0);
        // 执行查询并用 Optional（可选值）包装。
        return Optional.ofNullable(projectMapper.selectOne(queryWrapper));
    }

    /**
     * listByUserId（按用户编号查询列表）用于读取用户项目列表。
     *
     * @param userId 用户编号
     * @return 项目实体列表
     */
    @Override
    public List<ProjectEntity> listByUserId(String userId) {
        // 创建按用户编号查询、排除逻辑删除并按创建时间倒序排列的条件。
        LambdaQueryWrapper<ProjectEntity> queryWrapper = new LambdaQueryWrapper<ProjectEntity>()
                // userId（用户编号）必须匹配。
                .eq(ProjectEntity::getUserId, userId)
                // deleted（逻辑删除）必须为正常。
                .eq(ProjectEntity::getDeleted, 0)
                // 按创建时间倒序排列。
                .orderByDesc(ProjectEntity::getCreateTime);
        // 执行查询并返回列表。
        return projectMapper.selectList(queryWrapper);
    }

    /**
     * newId（生成编号）用于生成带业务前缀的随机编号。
     *
     * @param prefix 编号前缀
     * @return 随机编号
     */
    private String newId(String prefix) {
        // 使用 UUID（通用唯一识别码）生成本地开发阶段足够唯一的编号。
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
