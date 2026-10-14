package com.clipforge.creation.planner;

import com.clipforge.creation.model.AgentContext;
import com.clipforge.creation.model.AgentPlan;
import com.clipforge.creation.model.AgentStep;
import com.clipforge.observability.trace.TraceRecorder;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * FixedCreationPlanner（固定创作规划器）。
 *
 * <p>该规划器先用固定规则生成 AgentPlan（智能体计划），后续可以替换为 LlmCreationPlanner（大模型创作规划器）。
 */
@Component
@RequiredArgsConstructor
public class FixedCreationPlanner implements CreationPlanner {

    /** traceRecorder（执行链路记录器）用于记录规划过程。 */
    private final TraceRecorder traceRecorder;

    /**
     * plan（规划）用于生成当前阶段的固定 AgentPlan（智能体计划）。
     *
     * @param context 智能体上下文
     * @return 智能体计划
     */
    @Override
    public AgentPlan plan(AgentContext context) {
        // 记录规划开始事件。
        traceRecorder.record(context.getRunId(), "PLANNER_STARTED", "规划器开始生成创作计划", context.getGoal());
        // 创建固定步骤列表，表示内容创作内部要完成的任务。
        List<AgentStep> steps = List.of(
                // 第一步分析输入，明确用户目标和平台限制。
                new AgentStep("ANALYZE_INPUT", "分析创作目标、目标平台和风格偏好"),
                // 第二步结合知识，确定创作主题和受众。
                new AgentStep("DEFINE_THEME", "结合创作知识确定主题和目标受众"),
                // 第三步设计开头，提高前三秒吸引力。
                new AgentStep("DESIGN_HOOK", "设计开头钩子"),
                // 第四步组织内容结构，形成分镜和旁白。
                new AgentStep("WRITE_SCRIPT", "组织分镜、旁白和字幕建议"),
                // 第五步适配发布场景，生成标题和话题标签。
                new AgentStep("PREPARE_PUBLISH_COPY", "生成标题候选、发布文案和话题标签"));
        // 创建唯一计划编号，用于后续追踪。
        String planId = UUID.randomUUID().toString();
        // 创建智能体计划对象。
        AgentPlan plan = new AgentPlan(planId, "为平台 " + context.getPlatform() + " 规划内容创作任务", steps);
        // 记录规划完成事件。
        traceRecorder.record(context.getRunId(), "PLANNER_FINISHED", "规划器完成创作计划", "步骤数：" + steps.size());
        // 返回智能体计划。
        return plan;
    }
}
