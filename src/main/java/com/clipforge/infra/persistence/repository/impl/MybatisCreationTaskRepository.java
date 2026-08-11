package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.clipforge.infra.persistence.entity.CreationTaskEntity;
import com.clipforge.infra.persistence.enums.CreationTaskStatus;
import com.clipforge.infra.persistence.mapper.CreationTaskMapper;
import com.clipforge.infra.persistence.repository.CreationTaskRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisCreationTaskRepository（MyBatis 创作任务仓储实现）。
 *
 * <p>该实现类只在 db（数据库）Profile（配置环境）下启用，通过 MyBatis-Plus（MyBatis 增强工具）访问 PostgreSQL（关系数据库）。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisCreationTaskRepository implements CreationTaskRepository {

    /** creationTaskMapper（创作任务映射器）负责执行数据库 CRUD（增删改查）。 */
    private final CreationTaskMapper creationTaskMapper;

    /**
     * createTask（创建任务）用于保存一次创作请求的基础输入。
     *
     * @param runId Agent（智能体）运行编号
     * @param goal 创作目标
     * @param platform 目标平台
     * @param stylePreference 风格偏好
     * @return 已保存的创作任务实体
     */
    @Override
    public CreationTaskEntity createTask(String runId, String goal, String platform, String stylePreference) {
        // 获取当前时间，统一填充创建时间和更新时间。
        LocalDateTime now = LocalDateTime.now();
        // 使用 Builder（建造者模式）组装创作任务实体。
        CreationTaskEntity entity = CreationTaskEntity.builder()
                // 设置数据库内部主键。
                .id(newId("task_pk"))
                // 设置对外任务编号。
                .taskId(newId("task"))
                // 设置 Agent（智能体）运行编号。
                .runId(runId)
                // 设置用户创作目标。
                .goal(goal)
                // 设置目标平台。
                .platform(platform)
                // 设置风格偏好。
                .stylePreference(stylePreference)
                // 设置初始任务状态。
                .status(CreationTaskStatus.CREATED.name())
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记为正常。
                .deleted(0)
                // 构建实体对象。
                .build();
        // 调用 Mapper（映射器）插入数据库。
        creationTaskMapper.insert(entity);
        // 返回已保存实体，供后续保存结果时使用 taskId（任务编号）。
        return entity;
    }

    /**
     * markRunning（标记执行中）用于把任务状态更新为 RUNNING（执行中）。
     *
     * @param runId Agent（智能体）运行编号
     */
    @Override
    public void markRunning(String runId) {
        // 更新任务状态为执行中。
        updateStatus(runId, CreationTaskStatus.RUNNING, null);
    }

    /**
     * markSucceeded（标记成功）用于把任务状态更新为 SUCCEEDED（已成功）。
     *
     * @param runId Agent（智能体）运行编号
     */
    @Override
    public void markSucceeded(String runId) {
        // 更新任务状态为已成功。
        updateStatus(runId, CreationTaskStatus.SUCCEEDED, null);
    }

    /**
     * markFailed（标记失败）用于把任务状态更新为 FAILED（已失败）。
     *
     * @param runId Agent（智能体）运行编号
     * @param errorMessage 错误消息
     */
    @Override
    public void markFailed(String runId, String errorMessage) {
        // 更新任务状态为已失败，并保存错误消息。
        updateStatus(runId, CreationTaskStatus.FAILED, errorMessage);
    }

    /**
     * findByRunId（按运行编号查询）用于根据 Agent（智能体）运行编号查询任务。
     *
     * @param runId Agent（智能体）运行编号
     * @return 可能存在的创作任务实体
     */
    @Override
    public Optional<CreationTaskEntity> findByRunId(String runId) {
        // 创建按 runId（运行编号）查询且排除逻辑删除数据的条件。
        LambdaQueryWrapper<CreationTaskEntity> queryWrapper = new LambdaQueryWrapper<CreationTaskEntity>()
                // runId（运行编号）必须匹配。
                .eq(CreationTaskEntity::getRunId, runId)
                // deleted（逻辑删除）必须为正常。
                .eq(CreationTaskEntity::getDeleted, 0);
        // 执行数据库查询，并用 Optional（可选值）包装可能为空的结果。
        return Optional.ofNullable(creationTaskMapper.selectOne(queryWrapper));
    }

    /**
     * updateStatus（更新状态）用于统一处理任务状态流转。
     *
     * @param runId Agent（智能体）运行编号
     * @param status 目标任务状态
     * @param errorMessage 错误消息
     */
    private void updateStatus(String runId, CreationTaskStatus status, String errorMessage) {
        // 创建按 runId（运行编号）更新任务状态的条件。
        LambdaUpdateWrapper<CreationTaskEntity> updateWrapper = new LambdaUpdateWrapper<CreationTaskEntity>()
                // 设置任务状态。
                .set(CreationTaskEntity::getStatus, status.name())
                // 设置更新时间。
                .set(CreationTaskEntity::getUpdateTime, LocalDateTime.now())
                // 设置错误消息，成功和执行中时可以为空。
                .set(CreationTaskEntity::getErrorMessage, errorMessage)
                // 只更新指定运行编号的任务。
                .eq(CreationTaskEntity::getRunId, runId)
                // 只更新未删除任务。
                .eq(CreationTaskEntity::getDeleted, 0);
        // 执行更新语句。
        creationTaskMapper.update(null, updateWrapper);
    }

    /**
     * newId（生成编号）用于生成带业务前缀的随机编号。
     *
     * @param prefix 编号前缀
     * @return 随机编号
     */
    private String newId(String prefix) {
        // 使用 UUID（通用唯一识别码）生成足够唯一的本地开发阶段编号。
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
