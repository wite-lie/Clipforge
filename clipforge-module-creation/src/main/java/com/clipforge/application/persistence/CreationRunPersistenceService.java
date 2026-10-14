package com.clipforge.application.persistence;

import com.clipforge.contract.creation.CreationOutputView;
import com.clipforge.creation.model.AgentRunResult;

/**
 * CreationRunPersistenceService（创作运行持久化服务）。
 *
 * <p>该接口位于 Application（应用编排层），用于隔离业务主链路和具体数据库实现。
 */
public interface CreationRunPersistenceService {

    /**
     * createRunningTask（创建执行中任务）用于在 Agent（智能体）执行前保存任务记录。
     *
     * @param runId Agent（智能体）运行编号
     * @param goal 创作目标
     * @param platform 目标平台
     * @param stylePreference 风格偏好
     * @return 创作任务编号
     */
    String createRunningTask(String runId, String goal, String platform, String stylePreference);

    /**
     * saveSucceededRun（保存成功运行）用于在 Agent（智能体）执行成功后保存结果和执行链路。
     *
     * @param taskId 创作任务编号
     * @param runResult Agent（智能体）运行结果
     */
    CreationOutputView saveSucceededRun(String taskId, AgentRunResult runResult);

    /**
     * saveFailedRun（保存失败运行）用于在 Agent（智能体）执行失败后保存失败状态。
     *
     * @param runId Agent（智能体）运行编号
     * @param exception 异常对象
     */
    void saveFailedRun(String runId, Exception exception);
}
