package com.clipforge.agent.intent;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * HybridConversationIntentAgent（混合会话意图智能体）。
 *
 * <p>该实现先使用规则识别高置信度意图；规则不确定时，再按配置调用
 * LLMConversationIntentClassifier（大语言模型会话意图分类器）；模型不可用或置信度不足时，
 * 使用安全兜底策略，避免误触发昂贵或错误的 Workflow（工作流）。
 */
@Primary
@Component
@RequiredArgsConstructor
public class HybridConversationIntentAgent implements ConversationIntentAgent {

    /** ruleAgent（规则智能体）用于处理高置信度、低成本的常见意图。 */
    private final RuleBasedConversationIntentAgent ruleAgent;

    /** llmClassifier（大语言模型分类器）用于处理规则无法稳定识别的表达。 */
    private final LLMConversationIntentClassifier llmClassifier;

    /** properties（配置）用于控制启用开关和置信度阈值。 */
    private final ConversationIntentProperties properties;

    /**
     * recognize（识别）用于执行规则优先、模型补充、兜底保护的混合识别流程。
     */
    @Override
    public ConversationIntentDecision recognize(ConversationIntentRequest request) {
        if (!properties.isEnabled()) {
            return generateFallback("增强意图识别未启用，默认进入创作方案生成");
        }
        ConversationIntentDecision ruleDecision = ruleAgent.recognize(request);
        if (ruleDecision.getIntentType() != ConversationIntentType.UNKNOWN
                && ruleDecision.getConfidence() >= properties.getRuleConfidenceThreshold()) {
            return ruleDecision;
        }
        if (properties.isLlmEnabled()) {
            ConversationIntentDecision llmDecision = llmClassifier.classify(request);
            if (llmDecision.getIntentType() != ConversationIntentType.UNKNOWN
                    && llmDecision.getConfidence() >= properties.getLlmConfidenceThreshold()) {
                return normalizeRouteTarget(llmDecision);
            }
        }
        if (ruleDecision.getIntentType() != ConversationIntentType.UNKNOWN) {
            return ConversationIntentDecision.builder()
                    .intentType(ruleDecision.getIntentType())
                    .confidence(ruleDecision.getConfidence())
                    .recognitionMethod(ConversationIntentRecognitionMethod.FALLBACK)
                    .routeTarget(ruleDecision.getRouteTarget())
                    .requiredContext(ruleDecision.getRequiredContext())
                    .missingContext(ruleDecision.getMissingContext())
                    .reason("模型分类未采信，回退到低置信度规则结果：" + ruleDecision.getReason())
                    .build();
        }
        return ConversationIntentDecision.builder()
                .intentType(ConversationIntentType.CLARIFICATION)
                .confidence(0.55)
                .recognitionMethod(ConversationIntentRecognitionMethod.FALLBACK)
                .routeTarget("CLARIFICATION_HANDLER")
                .requiredContext(List.of("message"))
                .missingContext(List.of("message"))
                .reason("规则和模型都无法稳定判断，转入澄清")
                .build();
    }

    /**
     * normalizeRouteTarget（规范路由目标）用于补齐模型没有返回的路由目标。
     */
    private ConversationIntentDecision normalizeRouteTarget(ConversationIntentDecision decision) {
        String routeTarget = decision.getRouteTarget();
        if (routeTarget == null || routeTarget.isBlank()) {
            routeTarget = switch (decision.getIntentType()) {
                case GENERATE_PLAN, REVISE_PLAN -> "CREATION_WORKFLOW_HANDLER";
                case QUERY_MATERIAL_STATUS -> "QUERY_MATERIAL_STATUS_HANDLER";
                case PROCESS_MATERIAL -> "PROCESS_MATERIAL_HANDLER";
                case QUERY_USAGE_COST -> "USAGE_COST_HANDLER";
                case EXPLAIN_EVALUATION -> "QUALITY_REPORT_HANDLER";
                case KNOWLEDGE_QA -> "KNOWLEDGE_QA_HANDLER";
                case CHITCHAT -> "CHITCHAT_HANDLER";
                case CLARIFICATION, UNKNOWN, QUERY_HISTORY -> "CLARIFICATION_HANDLER";
            };
        }
        return ConversationIntentDecision.builder()
                .intentType(decision.getIntentType())
                .confidence(decision.getConfidence())
                .recognitionMethod(decision.getRecognitionMethod())
                .routeTarget(routeTarget)
                .requiredContext(decision.getRequiredContext())
                .missingContext(decision.getMissingContext())
                .reason(decision.getReason())
                .rawModelOutput(decision.getRawModelOutput())
                .build();
    }

    /**
     * generateFallback（生成兜底）用于增强识别关闭时保持旧创作路径。
     */
    private ConversationIntentDecision generateFallback(String reason) {
        return ConversationIntentDecision.builder()
                .intentType(ConversationIntentType.GENERATE_PLAN)
                .confidence(0.60)
                .recognitionMethod(ConversationIntentRecognitionMethod.FALLBACK)
                .routeTarget("CREATION_WORKFLOW_HANDLER")
                .requiredContext(List.of("message", "platform"))
                .missingContext(List.of())
                .reason(reason)
                .build();
    }
}
