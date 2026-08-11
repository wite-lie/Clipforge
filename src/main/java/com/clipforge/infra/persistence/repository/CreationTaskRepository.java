package com.clipforge.infra.persistence.repository;

import com.clipforge.infra.persistence.entity.CreationTaskEntity;
import java.util.Optional;

/**
 * CreationTaskRepository（创作任务仓储）。
 *
 * <p>该接口隐藏具体持久化技术，让应用层后续不直接依赖 MyBatis-Plus（MyBatis 增强工具）的 Mapper（映射器）。
 */
public interface CreationTaskRepository {

    /**
     * createTask（创建任务）用于保存一次创作请求的基础输入。
     *
     * @param runId Agent（智能体）运行编号
     * @param goal 创作目标
     * @param platform 目标平台
     * @param stylePreference 风格偏好
     * @return 已保存的创作任务实体
     */
    CreationTaskEntity createTask(String runId, String goal, String platform, String stylePreference);

    /**
     * markRunning（标记执行中）用于把任务状态更新为 RUNNING（执行中）。
     *
     * @param runId Agent（智能体）运行编号
     */
    void markRunning(String runId);

    /**
     * markSucceeded（标记成功）用于把任务状态更新为 SUCCEEDED（已成功）。
     *
     * @param runId Agent（智能体）运行编号
     */
    void markSucceeded(String runId);

    /**
     * markFailed（标记失败）用于把任务状态更新为 FAILED（已失败）。
     *
     * @param runId Agent（智能体）运行编号
     * @param errorMessage 错误消息
     */
    void markFailed(String runId, String errorMessage);

    /**
     * findByRunId（按运行编号查询）用于根据 Agent（智能体）运行编号查询任务。
     *
     * @param runId Agent（智能体）运行编号
     * @return 可能存在的创作任务实体
     */
    Optional<CreationTaskEntity> findByRunId(String runId);
}
