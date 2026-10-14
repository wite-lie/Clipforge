package com.clipforge.contract.knowledge;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ModelUsageSummaryResponse（模型用量汇总响应）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelUsageSummaryResponse {

    private String groupBy;

    private Integer totalCallCount;

    private Integer successCallCount;

    private Integer failedCallCount;

    private Integer totalPromptTokens;

    private Integer totalCompletionTokens;

    private Integer totalTokens;

    private Long totalLatencyMs;

    private Long averageLatencyMs;

    private BigDecimal estimatedInputCost;

    private BigDecimal estimatedOutputCost;

    private BigDecimal estimatedTotalCost;

    private String currency;

    private Boolean priceMatched;

    private List<ModelUsageGroupView> groups;
}
