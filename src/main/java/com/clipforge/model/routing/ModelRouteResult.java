package com.clipforge.model.routing;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * ModelRouteResult（模型路由结果）。
 *
 * <p>该对象保存最终模型返回值、命中的候选模型和完整尝试路径，方便调用方或测试做复盘。
 *
 * @param <T> 模型调用返回类型
 */
@Getter
@Builder
public class ModelRouteResult<T> {

    private final T value;
    private final ModelRouteCandidate<T> selectedCandidate;
    private final List<ModelRouteAttempt> attempts;

    /**
     * fallbackUsed（是否发生故障转移）用于判断本次调用是否经历了主模型失败后的备用模型切换。
     *
     * @return true 表示至少尝试了两个候选并最终成功
     */
    public boolean fallbackUsed() {
        return attempts != null && attempts.size() > 1 && selectedCandidate != null;
    }
}
