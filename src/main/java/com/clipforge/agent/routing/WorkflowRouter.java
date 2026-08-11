package com.clipforge.agent.routing;

import com.clipforge.agent.intent.ConversationIntentDecision;
import com.clipforge.agent.intent.ConversationIntentType;
import org.springframework.stereotype.Component;

/**
 * WorkflowRouter（工作流路由器）。
 *
 * <p>该组件把 ConversationIntentDecision（会话意图决策）转换为稳定的路由目标。
 * 它不直接调用业务服务，只负责把“用户想做什么”映射为“后端走哪条处理路径”。
 */
@Component
public class WorkflowRouter {

    /**
     * route（路由）用于根据意图决策返回处理器目标。
     */
    public String route(ConversationIntentDecision decision) {
        if (decision == null || decision.getIntentType() == null) {
            return "CLARIFICATION_HANDLER";
        }
        if (hasText(decision.getRouteTarget())) {
            return decision.getRouteTarget();
        }
        ConversationIntentType intentType = decision.getIntentType();
        return switch (intentType) {
            case GENERATE_PLAN, REVISE_PLAN -> "CREATION_WORKFLOW_HANDLER";
            case QUERY_MATERIAL_STATUS -> "QUERY_MATERIAL_STATUS_HANDLER";
            case PROCESS_MATERIAL -> "PROCESS_MATERIAL_HANDLER";
            case QUERY_USAGE_COST -> "USAGE_COST_HANDLER";
            case EXPLAIN_EVALUATION -> "QUALITY_REPORT_HANDLER";
            case KNOWLEDGE_QA -> "KNOWLEDGE_QA_HANDLER";
            case CHITCHAT -> "CHITCHAT_HANDLER";
            case CLARIFICATION, QUERY_HISTORY, UNKNOWN -> "CLARIFICATION_HANDLER";
        };
    }

    /**
     * hasText（是否有文本）用于判断路由目标是否有效。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
