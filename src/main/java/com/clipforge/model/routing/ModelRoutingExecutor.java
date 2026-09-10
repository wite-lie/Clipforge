package com.clipforge.model.routing;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.trace.TraceRecorder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * ModelRoutingExecutor（模型路由执行器）。
 *
 * <p>该组件统一执行“选择候选 -> 调用主模型 -> 记录健康状态 -> 失败时故障转移”的流程。
 */
@Component
@RequiredArgsConstructor
public class ModelRoutingExecutor {

    private final ModelRoutingProperties properties;
    private final ModelHealthStore healthStore;
    private final TraceRecorder traceRecorder;

    /**
     * execute（执行）根据路由配置和健康状态尝试候选模型，并返回成功结果。
     *
     * @param request 模型路由请求
     * @return 模型路由结果
     * @param <T> 模型调用返回类型
     */
    public <T> ModelRouteResult<T> execute(ModelRouteRequest<T> request) {
        validate(request);
        List<ModelRouteCandidate<T>> candidates = orderedCandidates(request);
        List<ModelRouteAttempt> attempts = new ArrayList<>();
        RuntimeException lastFailure = null;
        boolean hasHealthyCandidate = hasHealthyCandidate(request.getCapability(), candidates);
        for (ModelRouteCandidate<T> candidate : candidates) {
            if (shouldSkip(request.getCapability(), candidate, candidates.size(), hasHealthyCandidate)) {
                continue;
            }
            long startedAt = System.currentTimeMillis();
            try {
                recordTrace(request, "MODEL_ROUTE_ATTEMPT_STARTED", "模型路由开始尝试候选", detail(candidate, null));
//              真正调用模型
                T value = candidate.getSupplier().get();
                long latencyMs = System.currentTimeMillis() - startedAt;
                healthStore.markSuccess(request.getCapability(), candidate);
                attempts.add(ModelRouteAttempt.builder()
                        .candidateName(candidate.getCandidateName())
                        .provider(candidate.getProvider())
                        .modelName(candidate.getModelName())
                        .success(true)
                        .latencyMs(latencyMs)
                        .build());
                recordTrace(request, "MODEL_ROUTE_ATTEMPT_SUCCEEDED", "模型路由候选调用成功", detail(candidate, "latencyMs=" + latencyMs));
                return ModelRouteResult.<T>builder()
                        .value(value)
                        .selectedCandidate(candidate)
                        .attempts(List.copyOf(attempts))
                        .build();
            } catch (BusinessException exception) {
                lastFailure = exception;
                recordFailure(request, candidate, attempts, startedAt, exception);
            } catch (RuntimeException exception) {
                lastFailure = exception;
                recordFailure(request, candidate, attempts, startedAt, exception);
            } catch (Exception exception) {
                lastFailure = new BusinessException(ErrorCode.MODEL_CALL_ERROR,
                        "模型候选调用失败：" + exception.getMessage(), exception);
                recordFailure(request, candidate, attempts, startedAt, lastFailure);
            }
            if (!properties.isFailoverEnabled()) {
                break;
            }
        }
        String failureSummary = attempts.stream()
                .map(attempt -> attempt.getCandidateName() + ":" + attempt.getFailureReason())
                .toList()
                .toString();
        throw new BusinessException(ErrorCode.MODEL_CALL_ERROR,
                "所有模型路由候选均调用失败，capability=" + request.getCapability()
                        + "，bizId=" + safeText(request.getBizId()) + "，attempts=" + failureSummary,
                lastFailure);
    }

    private <T> void recordFailure(ModelRouteRequest<T> request,
                                   ModelRouteCandidate<T> candidate,
                                   List<ModelRouteAttempt> attempts,
                                   long startedAt,
                                   RuntimeException exception) {
        long latencyMs = System.currentTimeMillis() - startedAt;
        String failureReason = compact(exception.getMessage(), 300);
        healthStore.markFailure(request.getCapability(), candidate, failureReason);
        attempts.add(ModelRouteAttempt.builder()
                .candidateName(candidate.getCandidateName())
                .provider(candidate.getProvider())
                .modelName(candidate.getModelName())
                .success(false)
                .latencyMs(latencyMs)
                .failureReason(failureReason)
                .build());
        recordTrace(request, "MODEL_ROUTE_ATTEMPT_FAILED", "模型路由候选调用失败",
                detail(candidate, "latencyMs=" + latencyMs + "，failure=" + failureReason));
    }

    private <T> List<ModelRouteCandidate<T>> orderedCandidates(ModelRouteRequest<T> request) {
        List<String> providerOrder = properties.providerOrder(request.getCapability());
        List<ModelRouteCandidate<T>> candidates = new ArrayList<>(request.getCandidates());
        if (providerOrder.isEmpty()) {
            return candidates;
        }
        candidates.sort(Comparator.comparingInt(candidate -> providerRank(providerOrder, candidate.getProvider())));
        return candidates;
    }

    private int providerRank(List<String> providerOrder, String provider) {
        for (int index = 0; index < providerOrder.size(); index++) {
            if (providerOrder.get(index).equalsIgnoreCase(provider)) {
                return index;
            }
        }
        return providerOrder.size();
    }

    private boolean shouldSkip(ModelCapability capability, ModelRouteCandidate<?> candidate, int candidateSize, boolean hasHealthyCandidate) {
        if (candidateSize <= 1 || !properties.isSkipUnhealthyCandidate() || !hasHealthyCandidate) {
            return false;
        }
        return healthStore.find(capability, candidate)
                .map(snapshot -> !snapshot.healthy(properties.getUnhealthyFailureThreshold()))
                .orElse(false);
    }

    private boolean hasHealthyCandidate(ModelCapability capability, List<? extends ModelRouteCandidate<?>> candidates) {
        return candidates.stream()
                .anyMatch(candidate -> healthStore.find(capability, candidate)
                        .map(snapshot -> snapshot.healthy(properties.getUnhealthyFailureThreshold()))
                        .orElse(true));
    }

    private <T> void validate(ModelRouteRequest<T> request) {
        if (request == null || request.getCapability() == null) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "模型路由请求 capability（模型能力）不能为空");
        }
        if (request.getCandidates() == null || request.getCandidates().isEmpty()) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "模型路由候选列表不能为空，capability=" + request.getCapability());
        }
        if (request.getCandidates().stream().anyMatch(candidate -> candidate == null || candidate.getSupplier() == null)) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "模型路由候选不能为 null，且 supplier（调用供应器）不能为空");
        }
    }

    private <T> void recordTrace(ModelRouteRequest<T> request, String eventType, String message, String detail) {
        if (request.getTraceRunId() == null || request.getTraceRunId().isBlank()) {
            return;
        }
        traceRecorder.record(request.getTraceRunId(), eventType, message, detail);
    }

    private String detail(ModelRouteCandidate<?> candidate, String extra) {
        String base = "provider=" + candidate.getProvider()
                + "，candidate=" + candidate.getCandidateName()
                + "，model=" + safeText(candidate.getModelName());
        return extra == null || extra.isBlank() ? base : base + "，" + extra;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private String compact(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...<省略>";
    }
}
