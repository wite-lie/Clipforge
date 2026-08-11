package com.clipforge.evaluation;

import com.clipforge.agent.AgentContext;
import com.clipforge.agent.CreationPlanResult;
import com.clipforge.tool.CreativeKnowledge;
import com.clipforge.trace.TraceRecorder;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RuleBasedCreationPlanEvaluator（规则创作方案评估器）。
 *
 * <p>该评估器先用固定规则模拟评分，后续可以升级为 LLM-as-Judge（大模型裁判）。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clipforge.evaluation", name = "mode", havingValue = "rule", matchIfMissing = true)
public class RuleBasedCreationPlanEvaluator implements CreationPlanEvaluator {

    /** traceRecorder（执行链路记录器）用于记录评估过程。 */
    private final TraceRecorder traceRecorder;

    /** DEFAULT_REVISION_THRESHOLD（默认修正阈值）表示低于该综合分时建议触发 Reflection（反思修正）。 */
    private static final int DEFAULT_REVISION_THRESHOLD = 80;

    /**
     * evaluateStructured（结构化评估）用于给创作方案生成多维评估结果。
     *
     * @param context 智能体上下文
     * @param result 创作方案结果
     * @return 创作方案评估结果
     */
    @Override
    public CreationPlanEvaluationResult evaluateStructured(AgentContext context, CreationPlanResult result) {
        // 记录评估开始事件。
        traceRecorder.record(context.getRunId(), "EVALUATION_STARTED", "规则评估器开始多维评分", "平台：" + context.getPlatform());
        // 从上下文中读取创作知识。
        CreativeKnowledge knowledge = (CreativeKnowledge) context.getArtifact("CREATIVE_KNOWLEDGE");
        // 构建评估请求对象，后续真实大模型裁判可以复用同一输入结构。
        CreationPlanEvaluationRequest request = CreationPlanEvaluationRequest.builder()
                // 设置智能体上下文。
                .context(context)
                // 设置待评估创作方案。
                .planResult(result)
                // 设置创作知识。
                .creativeKnowledge(knowledge)
                // 构建请求。
                .build();
        // 把评估请求写入上下文，方便测试和 Trace（执行链路追踪）复盘。
        context.putArtifact("CREATION_PLAN_EVALUATION_REQUEST", request);
        // 构建多维评分列表。
        List<EvaluationDimensionScore> dimensionScores = buildDimensionScores(result, knowledge);
        // 创建改进建议列表。
        List<String> revisionAdvice = buildRevisionAdvice(knowledge);
        // 计算综合评分，当前规则阶段使用维度分平均值。
        int overallScore = averageScore(dimensionScores);
        // 构建结构化评估结果。
        CreationPlanEvaluationResult evaluationResult = CreationPlanEvaluationResult.builder()
                // 设置综合评分。
                .overallScore(overallScore)
                // 设置维度评分。
                .dimensionScores(dimensionScores)
                // 设置方案优点。
                .strengths(List.of("方案结构完整，覆盖主题、受众、分镜、旁白、字幕、标题和发布文案。", "已结合目标平台生成基础表达方向。"))
                // 设置方案不足。
                .weaknesses(List.of("当前仍缺少真实图片或视频素材支撑，画面细节偏通用。", "标题和前三秒钩子仍可进一步做差异化强化。"))
                // 设置是否需要修正。
                .revisionRequired(overallScore < DEFAULT_REVISION_THRESHOLD)
                // 设置修正建议。
                .revisionAdvice(revisionAdvice)
                // 设置评估器类型。
                .evaluatorType("rule")
                // 构建评估结果。
                .build();
        // 把结构化评估结果写入上下文。
        context.putArtifact("CREATION_PLAN_EVALUATION_RESULT", evaluationResult);
        // 记录评估完成事件。
        traceRecorder.record(context.getRunId(), "EVALUATION_FINISHED", "规则评估器完成多维评分", "综合评分：" + evaluationResult.getOverallScore() + "，是否需要修正：" + evaluationResult.isRevisionRequired());
        // 返回结构化评估结果。
        return evaluationResult;
    }

    /**
     * evaluate（评估）用于给创作方案回填综合分和改进建议，保持现有接口响应兼容。
     *
     * @param context 智能体上下文
     * @param result 创作方案结果
     * @return 评估后的创作方案结果
     */
    @Override
    public CreationPlanResult evaluate(AgentContext context, CreationPlanResult result) {
        // 生成结构化评估结果。
        CreationPlanEvaluationResult evaluationResult = evaluateStructured(context, result);
        // 把综合评分回填到创作方案结果，保持 V1（第一版）接口字段兼容。
        result.setScore(evaluationResult.getOverallScore());
        // 把修正建议回填到创作方案结果，保持 V1（第一版）接口字段兼容。
        result.setImprovementSuggestions(evaluationResult.getRevisionAdvice());
        // 把评估后的结果写入上下文。
        context.putArtifact("EVALUATED_PLAN_RESULT", result);
        // 返回评估后的结果。
        return result;
    }

    /**
     * buildDimensionScores（构建维度评分）用于生成当前规则版本的六个评分维度。
     *
     * @param result 创作方案结果
     * @param knowledge 创作知识
     * @return 维度评分列表
     */
    private List<EvaluationDimensionScore> buildDimensionScores(CreationPlanResult result, CreativeKnowledge knowledge) {
        // 创建维度评分列表。
        List<EvaluationDimensionScore> scores = new ArrayList<>();
        // 添加开头吸引力评分。
        scores.add(score("hookStrength", "开头吸引力", hasText(result.getHook()) ? 84 : 68, "根据开头钩子是否明确痛点和吸引用户停留进行评分。"));
        // 添加平台适配度评分。
        scores.add(score("platformFit", "平台适配度", knowledge == null ? 78 : 84, "根据目标平台知识是否进入评估上下文进行评分。"));
        // 添加素材相关性评分，当前文本阶段保守给分。
        scores.add(score("materialRelevance", "素材相关性", 76, "当前阶段尚未接入图片或视频素材理解，因此素材相关性保守评分。"));
        // 添加脚本完整度评分。
        scores.add(score("scriptCompleteness", "脚本完整度", hasContent(result.getStoryboard()) && hasText(result.getVoiceoverScript()) ? 86 : 70, "根据分镜脚本和旁白脚本是否完整进行评分。"));
        // 添加执行可行性评分。
        scores.add(score("executionFeasibility", "执行可行性", 82, "当前方案包含可执行的标题、字幕、发布文案和话题标签。"));
        // 添加传播或转化价值评分。
        scores.add(score("commercialValue", "传播或转化价值", hasContent(result.getTitleCandidates()) && hasText(result.getPublishCopy()) ? 80 : 72, "根据标题候选和发布文案是否具备传播表达进行评分。"));
        // 返回维度评分列表。
        return scores;
    }

    /**
     * buildRevisionAdvice（构建修正建议）用于生成可回填到创作方案结果的改进建议。
     *
     * @param knowledge 创作知识
     * @return 修正建议列表
     */
    private List<String> buildRevisionAdvice(CreativeKnowledge knowledge) {
        // 创建改进建议列表。
        List<String> suggestions = new ArrayList<>();
        // 添加素材增强建议。
        suggestions.add("后续可以结合真实图片素材增强画面描述。");
        // 添加知识库增强建议。
        suggestions.add("接入平台知识库后可进一步优化标题和标签。");
        // 读取第一条平台评估关注点。
        String firstEvaluationHint = firstEvaluationHint(knowledge);
        // 如果存在平台评估关注点，则写入建议。
        if (hasText(firstEvaluationHint)) {
            // 添加第一条评估提示，让评分结果体现知识检索结果。
            suggestions.add("平台评估关注点：" + firstEvaluationHint);
        }
        // 返回修正建议列表。
        return suggestions;
    }

    /**
     * score（创建维度评分）用于减少维度评分对象构造样板代码。
     *
     * @param dimension 维度编码
     * @param dimensionName 维度名称
     * @param score 分数
     * @param reason 原因
     * @return 维度评分
     */
    private EvaluationDimensionScore score(String dimension, String dimensionName, int score, String reason) {
        // 使用 Builder（建造者模式）创建维度评分。
        return EvaluationDimensionScore.builder()
                // 设置维度编码。
                .dimension(dimension)
                // 设置维度中文名称。
                .dimensionName(dimensionName)
                // 设置维度分数。
                .score(score)
                // 设置评分原因。
                .reason(reason)
                // 构建对象。
                .build();
    }

    /**
     * averageScore（平均分）用于从维度评分计算综合评分。
     *
     * @param dimensionScores 维度评分列表
     * @return 综合评分
     */
    private int averageScore(List<EvaluationDimensionScore> dimensionScores) {
        // 如果维度列表为空，则返回 0 分。
        if (dimensionScores == null || dimensionScores.isEmpty()) {
            // 返回兜底分数。
            return 0;
        }
        // 计算维度总分。
        int totalScore = dimensionScores.stream().mapToInt(EvaluationDimensionScore::getScore).sum();
        // 返回四舍五入后的平均分。
        return Math.round((float) totalScore / dimensionScores.size());
    }

    /**
     * firstEvaluationHint（第一条评估提示）用于安全读取知识库中的评估提示。
     *
     * @param knowledge 创作知识
     * @return 第一条评估提示
     */
    private String firstEvaluationHint(CreativeKnowledge knowledge) {
        // 如果知识为空，则返回空值。
        if (knowledge == null || knowledge.getEvaluationHints() == null || knowledge.getEvaluationHints().isEmpty()) {
            // 返回空值。
            return null;
        }
        // 返回第一条评估提示。
        return knowledge.getEvaluationHints().get(0);
    }

    /**
     * hasContent（是否有内容）用于判断列表是否包含元素。
     *
     * @param values 字符串列表
     * @return 是否有内容
     */
    private boolean hasContent(List<String> values) {
        // 判断列表不为空且至少存在一个元素。
        return values != null && !values.isEmpty();
    }

    /**
     * hasText（是否有文本）用于判断字符串是否包含非空白内容。
     *
     * @param value 字符串
     * @return 是否有文本
     */
    private boolean hasText(String value) {
        // 判断字符串不为空且去掉空白后仍有内容。
        return value != null && !value.isBlank();
    }
}
