package com.clipforge.ai.infrastructure.routing;
import com.clipforge.ai.api.routing.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * InMemoryModelHealthStore（内存模型健康状态存储）。
 *
 * <p>该实现用于单 JVM（Java 虚拟机）部署和本地开发。它记录每个候选模型的连续失败次数，并在成功后清零。
 */
@Component
public class InMemoryModelHealthStore implements ModelHealthStore {

    private final Map<String, ModelHealthSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public Optional<ModelHealthSnapshot> find(ModelCapability capability, ModelRouteCandidate<?> candidate) {
        return Optional.ofNullable(snapshots.get(routeKey(capability, candidate)));
    }

    @Override
    public void markSuccess(ModelCapability capability, ModelRouteCandidate<?> candidate) {
        String routeKey = routeKey(capability, candidate);
        snapshots.put(routeKey, ModelHealthSnapshot.builder()
                .routeKey(routeKey)
                .capability(capability)
                .provider(candidate.getProvider())
                .candidateName(candidate.getCandidateName())
                .consecutiveFailureCount(0)
                .lastSuccessAt(OffsetDateTime.now())
                .build());
    }

    @Override
    public void markFailure(ModelCapability capability, ModelRouteCandidate<?> candidate, String failureReason) {
        String routeKey = routeKey(capability, candidate);
        ModelHealthSnapshot oldSnapshot = snapshots.get(routeKey);
        int nextFailureCount = oldSnapshot == null ? 1 : oldSnapshot.getConsecutiveFailureCount() + 1;
        snapshots.put(routeKey, ModelHealthSnapshot.builder()
                .routeKey(routeKey)
                .capability(capability)
                .provider(candidate.getProvider())
                .candidateName(candidate.getCandidateName())
                .consecutiveFailureCount(nextFailureCount)
                .lastSuccessAt(oldSnapshot == null ? null : oldSnapshot.getLastSuccessAt())
                .lastFailureAt(OffsetDateTime.now())
                .lastFailureReason(failureReason)
                .build());
    }

    private String routeKey(ModelCapability capability, ModelRouteCandidate<?> candidate) {
        String safeCapability = capability == null ? "unknown" : capability.configKey();
        String safeProvider = blankToDefault(candidate == null ? null : candidate.getProvider(), "unknown");
        String safeName = blankToDefault(candidate == null ? null : candidate.getCandidateName(), "unknown");
        return safeCapability + ":" + safeProvider + ":" + safeName;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
