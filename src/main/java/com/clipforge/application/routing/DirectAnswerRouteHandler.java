package com.clipforge.application.routing;

import com.clipforge.agent.intent.ConversationIntentDecision;
import com.clipforge.api.dto.ConversationRouteRequest;
import com.clipforge.api.dto.ConversationRouteResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * DirectAnswerRouteHandler（直接答复路由处理器）用于处理知识问答提示、闲聊、澄清和未知意图。
 */
@Component
public class DirectAnswerRouteHandler extends AbstractConversationRouteHandler {

    /**
     * supports（是否支持）用于匹配不需要调用复杂业务服务的路由目标。
     */
    @Override
    public boolean supports(String routeTarget) {
        return "KNOWLEDGE_QA_HANDLER".equals(routeTarget)
                || "CHITCHAT_HANDLER".equals(routeTarget)
                || "CLARIFICATION_HANDLER".equals(routeTarget)
                || "FALLBACK_HANDLER".equals(routeTarget);
    }

    /**
     * handle（处理）用于返回轻量文本答复。
     */
    @Override
    public ConversationRouteResponse handle(ConversationRouteRequest request, ConversationIntentDecision decision, String routeTarget) {
        if ("KNOWLEDGE_QA_HANDLER".equals(routeTarget)) {
            return directAnswer(decision, routeTarget, "知识问答会在 RAG（检索增强生成）专用问答入口中处理；当前创作聊天入口已识别到知识问答意图，请选择知识库后发起问答或改为创作需求。", List.of());
        }
        if ("CHITCHAT_HANDLER".equals(routeTarget)) {
            return directAnswer(decision, routeTarget, "你好，我在。你可以上传图片或视频素材，也可以直接告诉我想做什么短视频。", List.of());
        }
        return directAnswer(decision, routeTarget, clarificationText(decision), safeList(decision == null ? null : decision.getMissingContext()));
    }

    /**
     * clarificationText（澄清文本）用于生成统一追问。
     */
    private String clarificationText(ConversationIntentDecision decision) {
        if (decision != null && decision.getMissingContext() != null && !decision.getMissingContext().isEmpty()) {
            return "我还需要你补充：" + String.join("、", decision.getMissingContext()) + "。";
        }
        return "我还不能稳定判断你是想生成方案、修改方案、查询素材状态，还是查询成本。请再补充一句你的具体目标。";
    }
}
