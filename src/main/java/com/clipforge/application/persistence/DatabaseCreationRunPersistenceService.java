package com.clipforge.application.persistence;

import com.clipforge.agent.AgentRunResult;
import com.clipforge.infra.persistence.entity.CreationTaskEntity;
import com.clipforge.infra.persistence.repository.AgentTraceEventRepository;
import com.clipforge.infra.persistence.repository.CreationResultRepository;
import com.clipforge.infra.persistence.repository.CreationTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DatabaseCreationRunPersistenceService（数据库创作运行持久化服务）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，负责把任务、结果和 Trace（执行链路追踪）保存到 PostgreSQL（关系数据库）。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class DatabaseCreationRunPersistenceService implements CreationRunPersistenceService {

    /** creationTaskRepository（创作任务仓储）负责保存和更新任务状态。 */
    private final CreationTaskRepository creationTaskRepository;

    /** creationResultRepository（创作结果仓储）负责保存创作方案结果。 */
    private final CreationResultRepository creationResultRepository;

    /** agentTraceEventRepository（智能体执行链路事件仓储）负责保存执行链路事件。 */
    private final AgentTraceEventRepository agentTraceEventRepository;

    /**
     * createRunningTask（创建执行中任务）用于在 Agent（智能体）执行前保存任务记录并标记执行中。
     *
     * @param runId Agent（智能体）运行编号
     * @param goal 创作目标
     * @param platform 目标平台
     * @param stylePreference 风格偏好
     * @return 创作任务编号
     */
    @Override
    @Transactional
    public String createRunningTask(String runId, String goal, String platform, String stylePreference) {
        // 创建任务记录，初始状态为 CREATED（已创建）。
        CreationTaskEntity taskEntity = creationTaskRepository.createTask(runId, goal, platform, stylePreference);
        // 立即标记为 RUNNING（执行中），表示 Agent（智能体）将开始处理。
        creationTaskRepository.markRunning(runId);
        // 返回任务编号，后续保存创作结果时需要关联它。
        return taskEntity.getTaskId();
    }

    /**
     * saveSucceededRun（保存成功运行）用于在 Agent（智能体）执行成功后保存结果、执行链路并标记成功。
     *
     * @param taskId 创作任务编号
     * @param runResult Agent（智能体）运行结果
     */
    @Override
    @Transactional
    public void saveSucceededRun(String taskId, AgentRunResult runResult) {
        // 保存创作方案结果。
        creationResultRepository.saveResult(taskId, runResult.getRunId(), runResult.getResult());
        // 保存本次 Agent（智能体）运行的执行链路事件。
        agentTraceEventRepository.saveEvents(runResult.getTraceEvents());
        // 更新任务状态为 SUCCEEDED（已成功）。
        creationTaskRepository.markSucceeded(runResult.getRunId());
    }

    /**
     * saveFailedRun（保存失败运行）用于在 Agent（智能体）执行失败后保存失败状态。
     *
     * @param runId Agent（智能体）运行编号
     * @param exception 异常对象
     */
    @Override
    @Transactional
    public void saveFailedRun(String runId, Exception exception) {
        // 标记任务失败，并保存异常消息作为失败原因。
        creationTaskRepository.markFailed(runId, exception.getMessage());
    }
}
