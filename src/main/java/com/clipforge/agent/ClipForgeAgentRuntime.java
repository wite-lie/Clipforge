package com.clipforge.agent;

import com.clipforge.agent.guardrail.CreationPlanOutputValidator;
import com.clipforge.agent.workflow.FixedCreationWorkflow;
import com.clipforge.trace.TraceEvent;
import com.clipforge.trace.TraceRecorder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ClipForgeAgentRuntime（创作规划智能体运行时）。
 *
 * <p>该组件负责创建 AgentContext（智能体上下文）、调用 Workflow（工作流）并汇总运行结果。
 */
@Component
@RequiredArgsConstructor
public class ClipForgeAgentRuntime {

    /** fixedCreationWorkflow（固定创作工作流）负责执行当前阶段的固定创作步骤。 */
    private final FixedCreationWorkflow fixedCreationWorkflow;

    /** traceRecorder（执行链路记录器）负责记录和读取本次智能体执行事件。 */
    private final TraceRecorder traceRecorder;

    /** creationPlanOutputValidator（创作方案输出校验器）负责在返回前检查智能体输出完整性。 */
    private final CreationPlanOutputValidator creationPlanOutputValidator;

    /**
     * run（运行）用于执行一次最小创作规划智能体链路。
     *
     * @param goal 创作目标
     * @param platform 目标平台
     * @param stylePreference 风格偏好
     * @return 智能体运行结果
     */
    public AgentRunResult run(String goal, String platform, String stylePreference) {
        // 生成运行编号，用于串联上下文、计划、结果和执行链路。
        String runId = UUID.randomUUID().toString();
        // 使用指定运行编号执行智能体链路。
        return runWithRunId(runId, goal, platform, stylePreference);
    }

    /**
     * runWithRunId（使用指定运行编号运行）用于让应用层提前生成 runId（运行编号），从而串联任务表、结果表和执行链路表。
     *
     * @param runId 运行编号
     * @param goal 创作目标
     * @param platform 目标平台
     * @param stylePreference 风格偏好
     * @return 智能体运行结果
     */
    public AgentRunResult runWithRunId(String runId, String goal, String platform, String stylePreference) {
        // 创建 AgentContext（智能体上下文），保存本次运行的输入。
        AgentContext context = new AgentContext(runId, goal, platform, stylePreference);
        // 使用上下文执行智能体链路。
        return runWithContext(context);
    }

    /**
     * runWithRunId（使用指定运行编号和元数据运行）用于把项目、会话等外部上下文传入 AgentContext（智能体上下文）。
     *
     * @param runId 运行编号
     * @param goal 创作目标
     * @param platform 目标平台
     * @param stylePreference 风格偏好
     * @param metadata 元数据
     * @return 智能体运行结果
     */
    public AgentRunResult runWithRunId(String runId, String goal, String platform, String stylePreference, Map<String, Object> metadata) {
        // 创建 AgentContext（智能体上下文），保存本次运行的输入。
        AgentContext context = new AgentContext(runId, goal, platform, stylePreference);
        // 如果元数据不为空，则写入上下文产物，供模型用量记录等节点读取。
        if (metadata != null) {
            // 遍历写入元数据。
            metadata.forEach(context::putArtifact);
        }
        // 使用上下文执行智能体链路。
        return runWithContext(context);
    }

    /**
     * runWithContext（使用上下文运行）用于执行完整智能体链路。
     *
     * @param context 智能体上下文
     * @return 智能体运行结果
     */
    private AgentRunResult runWithContext(AgentContext context) {
        // 读取运行编号。
        String runId = context.getRunId();
        // 读取目标平台。
        String platform = context.getPlatform();
        // 记录智能体运行开始事件。
        traceRecorder.record(runId, "AGENT_STARTED", "智能体开始执行", "目标平台：" + platform);
        // 调用固定工作流完成知识检索、规划、生成和评估。
        CreationWorkflowResult workflowResult = fixedCreationWorkflow.run(context);
        // 从工作流结果中读取 AgentPlan（智能体计划）。
        AgentPlan plan = workflowResult.getPlan();
        // 从工作流结果中读取 CreationPlanResult（创作方案结果）。
        CreationPlanResult result = workflowResult.getResult();
        // 校验创作方案输出，避免把缺字段或越界评分的结果返回给调用方。
        creationPlanOutputValidator.validate(result);
        // 记录智能体运行完成事件。
        traceRecorder.record(runId, "AGENT_FINISHED", "智能体执行完成", "综合评分：" + result.getScore());
        // 读取本次运行的完整执行链路。
        List<TraceEvent> traceEvents = traceRecorder.findByRunId(runId);
        // 返回智能体运行汇总结果。
        return new AgentRunResult(runId, plan, result, traceEvents);
    }
}
