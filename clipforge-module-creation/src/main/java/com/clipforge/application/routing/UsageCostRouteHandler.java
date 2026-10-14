package com.clipforge.application.routing;

import com.clipforge.agent.intent.ConversationIntentDecision;
import com.clipforge.contract.conversation.ConversationRouteRequest;
import com.clipforge.contract.knowledge.ConversationRouteResponse;
import com.clipforge.contract.knowledge.ModelUsageSummaryResponse;
import com.clipforge.application.ModelUsageQueryApplicationService;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.observability.usage.ModelCallRecordQuery;
import com.clipforge.observability.usage.ModelUsageGroupBy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * UsageCostRouteHandler（用量成本路由处理器）用于查询模型调用次数、Token（词元）和估算成本。
 */
@Component
@RequiredArgsConstructor
public class UsageCostRouteHandler extends AbstractConversationRouteHandler {

    /** modelUsageQueryApplicationService（模型用量查询应用服务）用于查询模型成本。 */
    private final ObjectProvider<ModelUsageQueryApplicationService> modelUsageQueryApplicationServiceProvider;

    /**
     * supports（是否支持）用于匹配模型用量成本路由目标。
     */
    @Override
    public boolean supports(String routeTarget) {
        return "USAGE_COST_HANDLER".equals(routeTarget);
    }

    /**
     * handle（处理）用于按 runId、projectId 或 conversationId 查询模型用量汇总。
     */
    @Override
    public ConversationRouteResponse handle(ConversationRouteRequest request, ConversationIntentDecision decision, String routeTarget) {
        if (!hasAnyScope(request)) {
            return directAnswer(decision, routeTarget, "查询模型用量需要至少提供 runId（运行编号）、projectId（项目编号）或 conversationId（会话编号）。", List.of("runId 或 projectId 或 conversationId"));
        }
        ModelUsageQueryApplicationService service = requiredModelUsageService();
        ModelUsageSummaryResponse usageSummary = service.querySummary(ModelCallRecordQuery.builder()
                .runId(request.getRunId())
                .projectId(request.getProjectId())
                .conversationId(request.getConversationId())
                .build(), ModelUsageGroupBy.PURPOSE);
        return base(decision, routeTarget)
                .answerText("本范围内模型调用 " + usageSummary.getTotalCallCount() + " 次，总 Token（词元）约 " + usageSummary.getTotalTokens() + "，估算成本 " + usageSummary.getEstimatedTotalCost() + " " + usageSummary.getCurrency() + "。")
                .usageSummary(usageSummary)
                .build();
    }

    /**
     * hasAnyScope（是否有查询范围）用于模型用量查询的边界保护。
     */
    private boolean hasAnyScope(ConversationRouteRequest request) {
        return request != null && (hasText(request.getRunId()) || hasText(request.getProjectId()) || hasText(request.getConversationId()));
    }

    /**
     * requiredModelUsageService（获取模型用量服务）用于处理未启用数据库环境。
     */
    private ModelUsageQueryApplicationService requiredModelUsageService() {
        ModelUsageQueryApplicationService service = modelUsageQueryApplicationServiceProvider.getIfAvailable();
        if (service == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "模型用量查询需要启用 db Profile（数据库配置环境）");
        }
        return service;
    }
}
