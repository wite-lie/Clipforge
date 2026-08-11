package com.clipforge.quality.record;

import com.clipforge.agent.AgentContext;
import com.clipforge.agent.CreationPlanResult;
import com.clipforge.evaluation.CreationPlanEvaluationResult;

/**
 * CreationQualityRecorder（创作质量记录器）。
 *
 * <p>该接口负责在 Workflow（工作流）执行过程中记录方案版本、评估结果、修正关系和最终版本。
 */
public interface CreationQualityRecorder {

    /**
     * recordInitialVersion（记录初始版本）用于保存第一次生成并评估后的创作方案版本。
     *
     * @param context 智能体上下文
     * @param result 创作方案结果
     * @return 方案版本编号
     */
    String recordInitialVersion(AgentContext context, CreationPlanResult result);

    /**
     * recordRevisedVersion（记录修正版本）用于保存根据评估建议修正后的创作方案版本。
     *
     * @param context 智能体上下文
     * @param result 修正后的创作方案结果
     * @param sourceVersionId 来源版本编号
     * @return 修正版本编号
     */
    String recordRevisedVersion(AgentContext context, CreationPlanResult result, String sourceVersionId);

    /**
     * recordEvaluation（记录评估）用于保存某个方案版本的结构化评估结果。
     *
     * @param context 智能体上下文
     * @param versionId 被评估的方案版本编号
     * @param evaluationResult 结构化评估结果
     * @return 评估结果编号
     */
    String recordEvaluation(AgentContext context, String versionId, CreationPlanEvaluationResult evaluationResult);

    /**
     * recordRevision（记录修正关系）用于保存一次修正动作的前后版本关系。
     *
     * @param context 智能体上下文
     * @param sourceVersionId 修正前版本编号
     * @param targetVersionId 修正后版本编号
     * @param evaluationId 触发修正的评估结果编号
     * @param evaluationResult 触发修正的评估结果
     * @return 修正记录编号
     */
    String recordRevision(AgentContext context, String sourceVersionId, String targetVersionId, String evaluationId, CreationPlanEvaluationResult evaluationResult);

    /**
     * markFinalVersion（标记最终版本）用于标记本次运行最终采用的方案版本。
     *
     * @param versionId 最终方案版本编号
     */
    void markFinalVersion(String versionId);
}
