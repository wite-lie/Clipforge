package com.clipforge.model.routing;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * ModelHealthSnapshot（模型健康快照）。
 *
 * <p>该对象记录某个模型候选近期成功/失败情况。当前实现保存在内存中，后续多实例部署可迁移到 Redis（缓存）
 * 或数据库。
 */
@Getter
@Builder
public class ModelHealthSnapshot {

    private final String routeKey;
    private final ModelCapability capability;
    private final String provider;
    private final String candidateName;
    private final int consecutiveFailureCount;
    private final OffsetDateTime lastSuccessAt;
    private final OffsetDateTime lastFailureAt;
    private final String lastFailureReason;

    /**
     * healthy（是否健康）根据连续失败次数和配置阈值判断当前候选是否可优先尝试。
     *
     * @param unhealthyFailureThreshold 连续失败阈值
     * @return true 表示当前候选可正常尝试
     */
    public boolean healthy(int unhealthyFailureThreshold) {
        return consecutiveFailureCount < Math.max(1, unhealthyFailureThreshold);
    }
}
