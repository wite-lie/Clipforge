package com.clipforge.api.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ModelUsageGroupView（模型用量分组视图）。
 *
 * <p>该对象表示模型用量统计接口中的一个分组结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelUsageGroupView {

    /** groupKey（分组键）表示当前分组的唯一文本键。 */
    private String groupKey;

    /** groupName（分组名称）表示当前分组的人类可读名称。 */
    private String groupName;

    /** callCount（调用次数）表示当前分组的模型调用数量。 */
    private Integer callCount;

    /** successCallCount（成功调用次数）表示当前分组成功调用数量。 */
    private Integer successCallCount;

    /** failedCallCount（失败调用次数）表示当前分组失败调用数量。 */
    private Integer failedCallCount;

    /** totalPromptTokens（总输入词元数）表示当前分组输入 Token（词元）消耗。 */
    private Integer totalPromptTokens;

    /** totalCompletionTokens（总输出词元数）表示当前分组输出 Token（词元）消耗。 */
    private Integer totalCompletionTokens;

    /** totalTokens（总词元数）表示当前分组总 Token（词元）消耗。 */
    private Integer totalTokens;

    /** totalLatencyMs（总耗时毫秒）表示当前分组总模型调用耗时。 */
    private Long totalLatencyMs;

    /** averageLatencyMs（平均耗时毫秒）表示当前分组平均模型调用耗时。 */
    private Long averageLatencyMs;

    /** estimatedInputCost（估算输入成本）表示当前分组输入 Token（词元）成本。 */
    private BigDecimal estimatedInputCost;

    /** estimatedOutputCost（估算输出成本）表示当前分组输出 Token（词元）成本。 */
    private BigDecimal estimatedOutputCost;

    /** estimatedTotalCost（估算总成本）表示当前分组总成本。 */
    private BigDecimal estimatedTotalCost;

    /** currency（币种）表示成本估算使用的货币单位。 */
    private String currency;

    /** priceMatched（是否命中价格）表示当前分组内记录是否全部命中价格配置。 */
    private Boolean priceMatched;
}
