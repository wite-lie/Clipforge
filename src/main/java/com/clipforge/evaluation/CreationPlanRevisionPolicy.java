package com.clipforge.evaluation;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CreationPlanRevisionPolicy（创作方案修正策略）。
 *
 * <p>该组件用于判断一次 Evaluation（评估）结果是否应该触发有限 Reflection（反思修正）。
 */
@Getter
@Component
public class CreationPlanRevisionPolicy {

    /** scoreThreshold（评分阈值）表示低于该综合评分时可以触发修正。 */
    @Value("${clipforge.evaluation.score-threshold:80}")
    private int scoreThreshold = 80;

    /** maxRevisionTimes（最大修正次数）用于防止 Reflection（反思修正）进入无限循环。 */
    @Value("${clipforge.evaluation.max-revision-times:1}")
    private int maxRevisionTimes = 1;

    /**
     * shouldRevise（是否应该修正）用于根据评估结果和当前修正次数做决策。
     *
     * @param evaluationResult 创作方案评估结果
     * @param currentRevisionTimes 当前已修正次数
     * @return 是否应该触发修正
     */
    public boolean shouldRevise(CreationPlanEvaluationResult evaluationResult, int currentRevisionTimes) {
        // 如果评估结果为空，则不能修正。
        if (evaluationResult == null) {
            // 返回不修正。
            return false;
        }
        // 如果当前修正次数已经达到上限，则不能继续修正。
        if (currentRevisionTimes >= maxRevisionTimes) {
            // 返回不修正。
            return false;
        }
        // 如果评估器明确要求修正，则触发修正。
        if (evaluationResult.isRevisionRequired()) {
            // 返回需要修正。
            return true;
        }
        // 如果综合评分低于阈值，则触发修正。
        return evaluationResult.getOverallScore() < scoreThreshold;
    }
}
