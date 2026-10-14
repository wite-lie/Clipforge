package com.clipforge.model.cost;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * ModelCostPrice（模型成本单价）。
 *
 * <p>该对象表示某个模型每 1000 Token（词元）的输入和输出价格。
 */
@Getter
@Setter
public class ModelCostPrice {

    /** inputPricePer1kTokens（每千输入词元价格）表示 Prompt（提示词）部分单价。 */
    private BigDecimal inputPricePer1kTokens = BigDecimal.ZERO;

    /** outputPricePer1kTokens（每千输出词元价格）表示模型生成部分单价。 */
    private BigDecimal outputPricePer1kTokens = BigDecimal.ZERO;
}
