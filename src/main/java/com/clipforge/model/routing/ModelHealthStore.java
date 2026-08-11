package com.clipforge.model.routing;

import java.util.Optional;

/**
 * ModelHealthStore（模型健康状态存储）。
 *
 * <p>该接口屏蔽健康状态的存储方式。当前用内存实现，未来可以替换为 Redis（缓存）以支持多实例共享健康状态。
 */
public interface ModelHealthStore {

    Optional<ModelHealthSnapshot> find(ModelCapability capability, ModelRouteCandidate<?> candidate);

    void markSuccess(ModelCapability capability, ModelRouteCandidate<?> candidate);

    void markFailure(ModelCapability capability, ModelRouteCandidate<?> candidate, String failureReason);
}
