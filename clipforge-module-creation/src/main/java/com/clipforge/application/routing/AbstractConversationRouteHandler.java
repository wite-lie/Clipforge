package com.clipforge.application.routing;

import com.clipforge.agent.intent.ConversationIntentDecision;
import com.clipforge.agent.intent.ConversationIntentType;
import com.clipforge.contract.knowledge.ConversationRouteResponse;
import java.util.List;

/**
 * AbstractConversationRouteHandler（抽象会话路由处理器）沉淀构建响应、文本判断等通用逻辑。
 */
abstract class AbstractConversationRouteHandler implements ConversationRouteHandler {

    /**
     * directAnswer（直接答复）用于构造不进入创作 Workflow（工作流）的响应。
     */
    protected ConversationRouteResponse directAnswer(ConversationIntentDecision decision, String routeTarget, String answerText, List<String> missingContext) {
        return base(decision, routeTarget)
                .answerText(answerText)
                .missingContext(missingContext)
                .build();
    }

    /**
     * base（基础响应）用于统一填充意图识别字段。
     */
    protected ConversationRouteResponse.ConversationRouteResponseBuilder base(ConversationIntentDecision decision, String routeTarget) {
        return ConversationRouteResponse.builder()
                .intentType(decision == null ? ConversationIntentType.UNKNOWN : decision.getIntentType())
                .confidence(decision == null ? 0.0 : decision.getConfidence())
                .recognitionMethod(decision == null ? null : decision.getRecognitionMethod())
                .routeTarget(routeTarget)
                .reason(decision == null ? null : decision.getReason());
    }

    /**
     * hasText（是否有文本）用于判断字符串是否包含有效内容。
     */
    protected boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * safeList（安全列表）用于把空列表兜底为不可变空列表。
     */
    protected List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
