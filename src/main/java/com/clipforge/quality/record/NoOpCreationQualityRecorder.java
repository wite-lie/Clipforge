package com.clipforge.quality.record;

import com.clipforge.agent.AgentContext;
import com.clipforge.agent.CreationPlanResult;
import com.clipforge.evaluation.CreationPlanEvaluationResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * NoOpCreationQualityRecorder（空创作质量记录器）。
 *
 * <p>该实现用于未启用 db（数据库）Profile（配置环境）时保持创作主链路可运行。
 */
@Component
@Profile("!db")
public class NoOpCreationQualityRecorder implements CreationQualityRecorder {

    @Override
    public String recordInitialVersion(AgentContext context, CreationPlanResult result) {
        return null;
    }

    @Override
    public String recordRevisedVersion(AgentContext context, CreationPlanResult result, String sourceVersionId) {
        return null;
    }

    @Override
    public String recordEvaluation(AgentContext context, String versionId, CreationPlanEvaluationResult evaluationResult) {
        return null;
    }

    @Override
    public String recordRevision(AgentContext context, String sourceVersionId, String targetVersionId, String evaluationId, CreationPlanEvaluationResult evaluationResult) {
        return null;
    }

    @Override
    public void markFinalVersion(String versionId) {
        // 非数据库环境不需要记录最终版本。
    }
}
