package com.clipforge.api.dto;

import com.clipforge.agent.intent.ConversationIntentRecognitionMethod;
import com.clipforge.agent.intent.ConversationIntentType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConversationRouteResponse（会话路由响应）。
 *
 * <p>该响应统一返回意图识别结果、路由目标、直接答复，以及可选的创作方案、任务状态、
 * 用量统计和质量复盘数据。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRouteResponse {

    /** intentType（意图类型）表示本轮用户消息识别出的意图。 */
    private ConversationIntentType intentType;

    /** confidence（置信度）表示意图识别把握程度。 */
    private Double confidence;

    /** recognitionMethod（识别方式）表示本轮意图来自规则、LLM（大语言模型）还是兜底。 */
    private ConversationIntentRecognitionMethod recognitionMethod;

    /** routeTarget（路由目标）表示后端实际选择的处理路径。 */
    private String routeTarget;

    /** answerText（答复文本）表示非创作路由或澄清路由直接返回给前端的文本。 */
    private String answerText;

    /** missingContext（缺失上下文）表示当前还缺少哪些字段。 */
    private List<String> missingContext;

    /** creation（创作结果）表示生成或修正方案路由返回的原始创作响应。 */
    private CreatePlanResponse creation;

    /** taskStatuses（任务状态列表）表示素材处理状态查询结果。 */
    private List<TaskStatusResponse> taskStatuses;

    /** usageSummary（用量汇总）表示模型调用 Token（词元）和成本统计。 */
    private ModelUsageSummaryResponse usageSummary;

    /** qualityReport（质量复盘）表示某次 Agent Run（智能体运行）的评估、修正和成本明细。 */
    private QueryQualityReportResponse qualityReport;

    /** reason（原因）表示意图识别或路由判断理由。 */
    private String reason;
}
