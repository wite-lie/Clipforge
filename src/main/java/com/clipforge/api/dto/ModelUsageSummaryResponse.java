package com.clipforge.api.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ModelUsageSummaryResponse（模型用量汇总响应）。
 *
 * <p>该对象用于返回模型调用次数、Token（词元）消耗、耗时和估算成本。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelUsageSummaryResponse {

    /** groupBy（分组维度）表示本次统计按哪个维度分组。 */
    private String groupBy;

    /** totalCallCount（总调用次数）表示命中查询条件的模型调用数量。 */
    private Integer totalCallCount;

    /** successCallCount（成功调用次数）表示成功模型调用数量。 */
    private Integer successCallCount;

    /** failedCallCount（失败调用次数）表示失败模型调用数量。 */
    private Integer failedCallCount;

    /** totalPromptTokens（总输入词元数）表示输入 Token（词元）消耗。 */
    private Integer totalPromptTokens;

    /** totalCompletionTokens（总输出词元数）表示输出 Token（词元）消耗。 */
    private Integer totalCompletionTokens;

    /** totalTokens（总词元数）表示总 Token（词元）消耗。 */
    private Integer totalTokens;

    /** totalLatencyMs（总耗时毫秒）表示模型调用总耗时。 */
    private Long totalLatencyMs;

    /** averageLatencyMs（平均耗时毫秒）表示平均模型调用耗时。 */
    private Long averageLatencyMs;

    /** estimatedInputCost（估算输入成本）表示输入 Token（词元）成本。 */
    private BigDecimal estimatedInputCost;

    /** estimatedOutputCost（估算输出成本）表示输出 Token（词元）成本。 */
    private BigDecimal estimatedOutputCost;

    /** estimatedTotalCost（估算总成本）表示输入和输出成本之和。 */
    private BigDecimal estimatedTotalCost;

    /** currency（币种）表示成本估算使用的货币单位。 */
    private String currency;

    /** priceMatched（是否全部命中价格）表示所有记录是否都找到了价格配置。 */
    private Boolean priceMatched;

    /** groups（分组明细）表示按指定维度聚合后的用量统计。 */
    private List<ModelUsageGroupView> groups;
}
