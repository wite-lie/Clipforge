package com.clipforge.application.persistence;

import com.clipforge.contract.creation.CreationOutputType;
import com.clipforge.contract.creation.CreationOutputView;
import com.clipforge.creation.model.AgentRunResult;
import com.clipforge.memory.infrastructure.persistence.entity.CreationOutputEntity;
import com.clipforge.memory.infrastructure.persistence.entity.CreationTaskEntity;
import com.clipforge.memory.infrastructure.persistence.repository.AgentTraceEventRepository;
import com.clipforge.memory.infrastructure.persistence.repository.CreationOutputRepository;
import com.clipforge.memory.infrastructure.persistence.repository.CreationResultRepository;
import com.clipforge.memory.infrastructure.persistence.repository.CreationTaskRepository;
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

    /** creationOutputRepository（创作产物仓储）负责保存通用创作产物。 */
    private final CreationOutputRepository creationOutputRepository;

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
    public CreationOutputView saveSucceededRun(String taskId, AgentRunResult runResult) {
        // 保存创作方案结果。
        creationResultRepository.saveResult(taskId, runResult.getRunId(), runResult.getResult());
        // 保存通用创作产物，后续 Markdown 导出和图文方案读取优先使用该表。
        CreationOutputEntity outputEntity = creationOutputRepository.saveOutput(taskId, runResult.getRunId(), runResult.getOutputType(), runResult.getOutputPayload() == null ? runResult.getResult() : runResult.getOutputPayload());
        // 保存本次 Agent（智能体）运行的执行链路事件。
        agentTraceEventRepository.saveEvents(runResult.getTraceEvents());
        // 更新任务状态为 SUCCEEDED（已成功）。
        creationTaskRepository.markSucceeded(runResult.getRunId());
        // 返回已保存产物视图，供创建接口直接返回 outputId（产物编号）。
        return toCreationOutputView(outputEntity);
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

    /**
     * toCreationOutputView（转创作产物视图）用于把数据库实体转换为接口响应对象。
     *
     * @param entity 创作产物实体
     * @return 创作产物视图
     */
    private CreationOutputView toCreationOutputView(CreationOutputEntity entity) {
        if (entity == null) {
            return null;
        }
        return CreationOutputView.builder()
                .outputId(entity.getOutputId())
                .runId(entity.getRunId())
                .taskId(entity.getTaskId())
                .outputType(CreationOutputType.resolveOrDefault(entity.getOutputType()))
                .schemaVersion(entity.getSchemaVersion())
                .payloadJson(entity.getPayloadJson())
                .createTime(entity.getCreateTime())
                .build();
    }
}
