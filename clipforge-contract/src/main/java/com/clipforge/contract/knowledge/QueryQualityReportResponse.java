package com.clipforge.contract.knowledge;

import com.clipforge.contract.creation.CreationPlanResult;
import com.clipforge.observability.trace.TraceEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * QueryQualityReportResponse（查询质量复盘响应）。
 *
 * <p>该对象聚合一次 Agent Run（智能体运行）的最终方案、评估链路、修正链路和 Token（词元）成本，方便 Phase 5（阶段 5）质量闭环复盘。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryQualityReportResponse {

    /** runId（运行编号）表示本次质量复盘对应的 Agent（智能体）运行。 */
    private String runId;

    /** taskId（任务编号）表示创作任务编号。 */
    private String taskId;

    /** resultId（结果编号）表示创作结果编号。 */
    private String resultId;

    /** finalResult（最终结果）表示持久化后的最终创作方案。 */
    private CreationPlanResult finalResult;

    /** finalScore（最终评分）表示最终创作方案的综合评分。 */
    private Integer finalScore;

    /** evaluationTriggered（是否触发评估）表示 Trace（执行链路追踪）中是否存在评估事件。 */
    private boolean evaluationTriggered;

    /** revisionTriggered（是否触发修正）表示 Trace（执行链路追踪）中是否存在修正开始事件。 */
    private boolean revisionTriggered;

    /** revisionCount（修正次数）表示本次运行实际触发修正的次数。 */
    private int revisionCount;

    /** totalPromptTokens（总输入词元数）表示本次运行全部模型调用的输入消耗。 */
    private Integer totalPromptTokens;

    /** totalCompletionTokens（总输出词元数）表示本次运行全部模型调用的输出消耗。 */
    private Integer totalCompletionTokens;

    /** totalTokens（总词元数）表示本次运行全部模型调用的合计消耗。 */
    private Integer totalTokens;

    /** tokensByPurpose（按用途汇总词元）表示生成、评估、修正等不同模型调用用途的 Token（词元）消耗。 */
    private Map<String, Integer> tokensByPurpose;

    /** estimatedInputCost（估算输入成本）表示本次运行输入 Token（词元）的估算成本。 */
    private BigDecimal estimatedInputCost;

    /** estimatedOutputCost（估算输出成本）表示本次运行输出 Token（词元）的估算成本。 */
    private BigDecimal estimatedOutputCost;

    /** estimatedTotalCost（估算总成本）表示本次运行全部模型调用的估算成本。 */
    private BigDecimal estimatedTotalCost;

    /** currency（币种）表示成本估算使用的货币单位。 */
    private String currency;

    /** priceMatched（是否全部命中价格）表示本次运行所有模型调用是否都找到了价格配置。 */
    private Boolean priceMatched;

    /** costByPurpose（按用途汇总成本）表示不同模型调用用途的估算成本分布。 */
    private Map<String, BigDecimal> costByPurpose;

    /** finalVersionId（最终版本编号）表示本次运行最终采用的创作方案版本。 */
    private String finalVersionId;

    /** versions（方案版本列表）表示本次运行的初始版、修正版和最终版标记。 */
    private List<CreationPlanVersionView> versions;

    /** evaluations（评估结果列表）表示每个方案版本对应的结构化评估结果。 */
    private List<CreationEvaluationResultView> evaluations;

    /** revisions（修正记录列表）表示修正前后版本关系。 */
    private List<CreationRevisionResultView> revisions;

    /** evaluationEvents（评估事件列表）表示和 Evaluation（评估）相关的执行链路事件。 */
    private List<TraceEvent> evaluationEvents;

    /** revisionEvents（修正事件列表）表示和 Reflection（反思修正）相关的执行链路事件。 */
    private List<TraceEvent> revisionEvents;

    /** modelCallRecords（模型调用记录）表示本次运行全部模型调用明细。 */
    private List<ModelCallRecordView> modelCallRecords;
}
