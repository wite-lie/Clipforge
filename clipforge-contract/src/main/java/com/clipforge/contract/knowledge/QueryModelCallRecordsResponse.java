package com.clipforge.contract.knowledge;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * QueryModelCallRecordsResponse（查询模型调用记录响应）。
 *
 * <p>该响应用于按 runId（运行编号）返回一次 Agent（智能体）运行中的全部模型调用明细和 Token（词元）汇总。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryModelCallRecordsResponse {

    /** runId（运行编号）表示本次查询对应的 Agent（智能体）运行。 */
    private String runId;

    /** totalPromptTokens（总输入词元数）表示本次运行全部模型调用的输入消耗。 */
    private Integer totalPromptTokens;

    /** totalCompletionTokens（总输出词元数）表示本次运行全部模型调用的输出消耗。 */
    private Integer totalCompletionTokens;

    /** totalTokens（总词元数）表示本次运行全部模型调用的合计消耗。 */
    private Integer totalTokens;

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

    /** records（记录列表）表示模型调用明细。 */
    private List<ModelCallRecordView> records;
}
