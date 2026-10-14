package com.clipforge.contract.knowledge;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ModelUsageGroupView（模型用量分组视图）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelUsageGroupView {

    private String groupKey;

    private String groupName;

    private Integer callCount;

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
}
