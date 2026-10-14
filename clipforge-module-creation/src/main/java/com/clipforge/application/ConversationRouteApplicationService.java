package com.clipforge.application;

import com.clipforge.agent.intent.ConversationIntentAgent;
import com.clipforge.agent.intent.ConversationIntentDecision;
import com.clipforge.agent.intent.ConversationIntentRequest;
import com.clipforge.agent.intent.ConversationIntentType;
import com.clipforge.agent.routing.WorkflowRouter;
import com.clipforge.contract.conversation.ConversationRouteRequest;
import com.clipforge.contract.knowledge.ConversationRouteResponse;
import com.clipforge.application.routing.ConversationRouteHandler;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * ConversationRouteApplicationService（会话路由应用服务）。
 *
 * <p>该服务是前端聊天框进入后端后的统一调度入口：先做 Intent Recognition（意图识别），
 * 再由 WorkflowRouter（工作流路由器）决定 routeTarget（路由目标），最后交给对应
 * ConversationRouteHandler（会话路由处理器）执行具体分支。
 */
@Service
@RequiredArgsConstructor
public class ConversationRouteApplicationService {

    /** conversationIntentAgent（会话意图智能体）用于判断当前轮次用户真实意图。 */
    private final ConversationIntentAgent conversationIntentAgent;

    /** workflowRouter（工作流路由器）用于把意图转换为稳定处理目标。 */
    private final WorkflowRouter workflowRouter;

    /** routeHandlers（路由处理器列表）用于承接不同 routeTarget（路由目标）的业务处理。 */
    private final List<ConversationRouteHandler> routeHandlers;

    /** projectConversationApplicationService（项目会话应用服务）用于保存非创作路由的用户消息和助手回复。 */
    private final ObjectProvider<ProjectConversationApplicationService> projectConversationApplicationServiceProvider;

    /**
     * route（路由）用于处理前端聊天框的一轮通用请求。
     *
     * @param request 会话路由请求
     * @return 会话路由响应
     */
    public ConversationRouteResponse route(ConversationRouteRequest request) {
        ConversationIntentDecision decision = conversationIntentAgent.recognize(toIntentRequest(request));
        String routeTarget = workflowRouter.route(decision);
        ConversationRouteResponse response = findHandler(routeTarget)
                .handle(request, normalizeDecision(decision), routeTarget);
        appendRouteConversationMessagesIfNeeded(request, response);
        return response;
    }

    /**
     * findHandler（查找处理器）用于根据 routeTarget（路由目标）获取具体处理器。
     */
    private ConversationRouteHandler findHandler(String routeTarget) {
        return routeHandlers.stream()
                .filter(handler -> handler.supports(routeTarget))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "未找到路由目标对应的处理器：" + routeTarget));
    }

    /**
     * normalizeDecision（规范化意图决策）用于把空决策统一转成 UNKNOWN（未知）意图。
     */
    private ConversationIntentDecision normalizeDecision(ConversationIntentDecision decision) {
        if (decision != null && decision.getIntentType() != null) {
            return decision;
        }
        return ConversationIntentDecision.builder()
                .intentType(ConversationIntentType.UNKNOWN)
                .confidence(0.0)
                .reason("意图识别结果为空，进入澄清路由")
                .build();
    }

    /**
     * appendRouteConversationMessagesIfNeeded（按需追加路由会话消息）用于让状态查询、成本查询、澄清等轻量路由也进入会话历史。
     *
     * <p>如果本次已经进入 CreationApplicationService（创作应用服务），则由创作服务负责保存用户消息和助手摘要，
     * 这里不重复写入，避免同一轮消息在 cf_conversation_message（会话消息表）中出现两次。
     */
    private void appendRouteConversationMessagesIfNeeded(ConversationRouteRequest request, ConversationRouteResponse response) {
        if (response != null && response.getCreation() != null) {
            return;
        }
        if (request == null || !hasText(request.getConversationId())) {
            return;
        }
        if (!hasText(request.getMessage())) {
            return;
        }
        ProjectConversationApplicationService service = projectConversationApplicationServiceProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        try {
            service.appendMessage(request.getConversationId(), request.getRunId(), "user", request.getMessage());
            if (response != null && hasText(response.getAnswerText())) {
                service.appendMessage(request.getConversationId(), request.getRunId(), "assistant", response.getAnswerText());
            }
        } catch (BusinessException exception) {
            if (exception.getErrorCode() != ErrorCode.PERSISTENCE_DISABLED) {
                throw exception;
            }
        }
    }

    /**
     * toIntentRequest（转意图请求）用于把接口请求转换为意图识别请求。
     */
    private ConversationIntentRequest toIntentRequest(ConversationRouteRequest request) {
        return ConversationIntentRequest.builder()
                .userId(request == null ? null : request.getUserId())
                .projectId(request == null ? null : request.getProjectId())
                .conversationId(request == null ? null : request.getConversationId())
                .runId(request == null ? null : request.getRunId())
                .message(request == null ? null : request.getMessage())
                .platform(request == null ? null : request.getPlatform())
                .stylePreference(request == null ? null : request.getStylePreference())
                .materialIds(request == null ? null : request.getMaterialIds())
                .processTaskIds(request == null ? null : request.getProcessTaskIds())
                .knowledgeBaseIds(request == null ? null : request.getKnowledgeBaseIds())
                .recentMessages(request == null ? null : request.getRecentMessages())
                .build();
    }

    /**
     * hasText（是否有文本）用于判断字符串是否包含有效内容。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
