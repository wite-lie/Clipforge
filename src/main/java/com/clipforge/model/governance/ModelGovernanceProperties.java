package com.clipforge.model.governance;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ModelGovernanceProperties（模型治理配置）用于集中控制真实模型调用的并发保护。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.model-governance")
public class ModelGovernanceProperties {

    /** enabled（是否启用）为 false 时，限流器直接放行所有模型调用。 */
    private boolean enabled = true;

    /** defaultMaxConcurrent（默认最大并发数）用于没有单独配置的模型调用类型。 */
    private int defaultMaxConcurrent = 3;

    /** defaultTimeoutMillis（默认等待超时毫秒数）用于没有单独配置等待时间的模型调用类型。 */
    private long defaultTimeoutMillis = 120_000L;

    /** limits（限流明细）按 ModelCallType.configKey（模型调用类型配置键）覆盖默认配置。 */
    private Map<String, ModelCallLimitProperties> limits = new LinkedHashMap<>();

    /**
     * maxConcurrentOf（读取最大并发数）返回指定调用类型的并发上限。
     *
     * @param callType 模型调用类型
     * @return 最大并发数
     */
    public int maxConcurrentOf(ModelCallType callType) {
        ModelCallLimitProperties limit = limits.get(callType.configKey());
        Integer configuredValue = limit == null ? null : limit.getMaxConcurrent();
        return Math.max(1, configuredValue == null ? defaultMaxConcurrent : configuredValue);
    }

    /**
     * timeoutMillisOf（读取等待超时）返回指定调用类型的等待超时时间。
     *
     * @param callType 模型调用类型
     * @return 等待超时毫秒数
     */
    public long timeoutMillisOf(ModelCallType callType) {
        ModelCallLimitProperties limit = limits.get(callType.configKey());
        Long configuredValue = limit == null ? null : limit.getTimeoutMillis();
        return Math.max(0L, configuredValue == null ? defaultTimeoutMillis : configuredValue);
    }
}
