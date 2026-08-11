package com.clipforge.model.cost;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ModelCallCost（模型调用成本）。
 *
 * <p>该对象保存一次模型调用或一组模型调用的输入成本、输出成本和总成本估算结果。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCallCost {

    /** inputCost（输入成本）表示 Prompt（提示词）Token（词元）估算成本。 */
    private BigDecimal inputCost;

    /** outputCost（输出成本）表示模型回答 Token（词元）估算成本。 */
    private BigDecimal outputCost;

    /** totalCost（总成本）表示输入和输出成本之和。 */
    private BigDecimal totalCost;

    /** currency（币种）表示成本估算使用的货币单位。 */
    private String currency;

    /** priceMatched（是否命中价格）表示是否找到供应商和模型对应的价格配置。 */
    private Boolean priceMatched;
}
