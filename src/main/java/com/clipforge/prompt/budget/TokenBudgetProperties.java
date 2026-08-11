package com.clipforge.prompt.budget;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TokenBudgetProperties（词元预算配置）。
 *
 * <p>该配置绑定 `clipforge.prompt.token-budget`，用于集中管理不同模型调用用途的上下文预算。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.prompt.token-budget")
public class TokenBudgetProperties {

    /** enabled（是否启用）表示是否生成 Token Budget（词元预算）决策。 */
    private boolean enabled = true;

    /** defaultContextWindow（默认上下文窗口）用于没有专属档案时兜底。 */
    private Integer defaultContextWindow = 32_768;

    /** defaultReservedOutputTokens（默认预留输出词元）用于为模型回答保留空间。 */
    private Integer defaultReservedOutputTokens = 4_096;

    /** defaultSafetyTokens（默认安全余量词元）用于覆盖估算误差和供应商差异。 */
    private Integer defaultSafetyTokens = 2_048;

    /** profiles（预算档案）用于按模型调用用途配置不同预算。 */
    private Map<String, TokenBudgetProfile> profiles = new LinkedHashMap<>();
}
