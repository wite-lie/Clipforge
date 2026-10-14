package com.clipforge.model.cost;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ModelCostProperties（模型成本配置）。
 *
 * <p>该配置用于按 provider（供应商）和 modelName（模型名称）配置模型调用单价，接口统计时只做估算，不代表供应商账单最终金额。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.model-cost")
public class ModelCostProperties {

    /** currency（币种）表示成本估算使用的货币单位。 */
    private String currency = "CNY";

    /** pricing（价格表）按供应商和模型名称保存单价。 */
    private Map<String, Map<String, ModelCostPrice>> pricing = new LinkedHashMap<>();
}
