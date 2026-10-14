package com.clipforge.application.routing;

import com.clipforge.agent.intent.ConversationIntentDecision;
import com.clipforge.contract.conversation.ConversationRouteRequest;
import com.clipforge.contract.knowledge.ConversationRouteResponse;

/**
 * ConversationRouteHandler（会话路由处理器）用于承接某一种路由目标的具体业务处理。
 *
 * <p>WorkflowRouter（工作流路由器）只负责选择 routeTarget（路由目标），
 * 具体调用哪个应用服务、如何组织响应，由不同处理器负责。
 */
public interface ConversationRouteHandler {

    /**
     * supports（是否支持）用于判断当前处理器是否能处理指定路由目标。
     *
     * @param routeTarget 路由目标
     * @return 是否支持
     */
    boolean supports(String routeTarget);

    /**
     * handle（处理）用于执行当前路由目标对应的业务逻辑。
     *
     * @param request 会话路由请求
     * @param decision 意图识别决策
     * @param routeTarget 路由目标
     * @return 会话路由响应
     */
    ConversationRouteResponse handle(ConversationRouteRequest request, ConversationIntentDecision decision, String routeTarget);
}
