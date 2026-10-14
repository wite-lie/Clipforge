package com.clipforge.application.persistence;

import com.clipforge.contract.creation.CreationOutputType;
import com.clipforge.contract.creation.CreationOutputView;
import com.clipforge.creation.model.AgentRunResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * NoOpCreationRunPersistenceService（空创作运行持久化服务）。
 *
 * <p>该实现只在非 db（数据库）Profile（配置环境）下启用，用于保持默认启动不依赖 PostgreSQL（关系数据库）。
 */
@Service
@Profile("!db")
public class NoOpCreationRunPersistenceService implements CreationRunPersistenceService {

    /**
     * createRunningTask（创建执行中任务）在默认环境下不写数据库，只返回一个内存任务编号。
     *
     * @param runId Agent（智能体）运行编号
     * @param goal 创作目标
     * @param platform 目标平台
     * @param stylePreference 风格偏好
     * @return 内存任务编号
     */
    @Override
    public String createRunningTask(String runId, String goal, String platform, String stylePreference) {
        // 默认环境不写数据库，返回一个可识别的内存任务编号。
        return "memory_task_" + runId;
    }

    /**
     * saveSucceededRun（保存成功运行）在默认环境下不执行任何持久化动作。
     *
     * @param taskId 创作任务编号
     * @param runResult Agent（智能体）运行结果
     */
    @Override
    public CreationOutputView saveSucceededRun(String taskId, AgentRunResult runResult) {
        // 默认环境不保存数据库，保留方法是为了让主链路代码保持一致。
        return CreationOutputView.builder()
                .taskId(taskId)
                .runId(runResult == null ? null : runResult.getRunId())
                .outputType(runResult == null ? CreationOutputType.VIDEO_PLAN : runResult.getOutputType())
                .schemaVersion("creation-output.v1")
                .build();
    }

    /**
     * saveFailedRun（保存失败运行）在默认环境下不执行任何持久化动作。
     *
     * @param runId Agent（智能体）运行编号
     * @param exception 异常对象
     */
    @Override
    public void saveFailedRun(String runId, Exception exception) {
        // 默认环境不保存数据库，异常继续由上层处理。
    }
}
