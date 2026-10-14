package com.clipforge.application.routing;

import com.clipforge.agent.intent.ConversationIntentDecision;
import com.clipforge.contract.conversation.ConversationRouteRequest;
import com.clipforge.contract.knowledge.ConversationRouteResponse;
import com.clipforge.contract.knowledge.QueryQualityReportResponse;
import com.clipforge.application.CreationQueryApplicationService;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * QualityReportRouteHandler（质量复盘路由处理器）用于查询一次创作运行的评估与修正细节。
 */
@Component
@RequiredArgsConstructor
public class QualityReportRouteHandler extends AbstractConversationRouteHandler {

    /** creationQueryApplicationService（创作查询应用服务）用于查询质量复盘。 */
    private final ObjectProvider<CreationQueryApplicationService> creationQueryApplicationServiceProvider;

    /**
     * supports（是否支持）用于匹配质量复盘路由目标。
     */
    @Override
    public boolean supports(String routeTarget) {
        return "QUALITY_REPORT_HANDLER".equals(routeTarget);
    }

    /**
     * handle（处理）用于根据 runId（运行编号）查询质量复盘。
     */
    @Override
    public ConversationRouteResponse handle(ConversationRouteRequest request, ConversationIntentDecision decision, String routeTarget) {
        if (request == null || !hasText(request.getRunId())) {
            return directAnswer(decision, routeTarget, "查询质量复盘需要提供 runId（运行编号）。", List.of("runId"));
        }
        QueryQualityReportResponse qualityReport = requiredCreationQueryService().queryQualityReportByRunId(request.getRunId());
        return base(decision, routeTarget)
                .answerText("已查询到质量复盘：最终评分 " + qualityReport.getFinalScore() + "，是否触发评估：" + qualityReport.isEvaluationTriggered() + "，是否触发修正：" + qualityReport.isRevisionTriggered() + "。")
                .qualityReport(qualityReport)
                .build();
    }

    /**
     * requiredCreationQueryService（获取创作查询服务）用于处理未启用数据库环境。
     */
    private CreationQueryApplicationService requiredCreationQueryService() {
        CreationQueryApplicationService service = creationQueryApplicationServiceProvider.getIfAvailable();
        if (service == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "质量复盘查询需要启用 db Profile（数据库配置环境）");
        }
        return service;
    }
}
