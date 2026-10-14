package com.clipforge.contract.knowledge;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConversationRouteResponse（会话路由响应）。
 *
 * <p>intentType 使用 Object 承接当前意图枚举，避免 contract 反向依赖具体 Agent 实现包。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRouteResponse {

    private Object intentType;

    private Double confidence;

    private Object recognitionMethod;

    private String routeTarget;

    private String reason;

    private String answerText;

    private List<String> missingContext;

    private CreatePlanResponse creation;

    private List<TaskStatusResponse> taskStatuses;

    private QueryQualityReportResponse qualityReport;

    private ModelUsageSummaryResponse usageSummary;
}
