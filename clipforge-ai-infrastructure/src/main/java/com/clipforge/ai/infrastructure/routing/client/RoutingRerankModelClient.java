package com.clipforge.ai.infrastructure.routing.client;

import com.clipforge.ai.infrastructure.rerank.BailianRerankModelClient;
import com.clipforge.ai.infrastructure.rerank.MockRerankModelClient;
import com.clipforge.ai.api.rerank.RerankModelClient;
import com.clipforge.ai.infrastructure.rerank.RerankModelProperties;
import com.clipforge.ai.api.rerank.RerankModelRequest;
import com.clipforge.ai.api.rerank.RerankModelResponse;
import com.clipforge.ai.infrastructure.governance.ModelCallLimiter;
import com.clipforge.ai.api.routing.ModelCapability;
import com.clipforge.ai.api.routing.ModelRouteCandidate;
import com.clipforge.ai.api.routing.ModelRouteRequest;
import com.clipforge.ai.infrastructure.routing.ModelRoutingExecutor;
import com.clipforge.ai.infrastructure.routing.ModelRoutingProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RoutingRerankModelClient（路由重排序模型客户端）。
 *
 * <p>该类对外仍实现 RerankModelClient（重排序模型客户端），内部按配置在百炼 Rerank（重排序）和 Mock（模拟）
 * 降级候选之间做故障转移。
 */
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clipforge.model-routing", name = "enabled", havingValue = "true")
public class RoutingRerankModelClient implements RerankModelClient {

    private final ObjectProvider<BailianRerankModelClient> realClientProvider;
    private final ObjectProvider<MockRerankModelClient> mockClientProvider;
    private final RerankModelProperties baseProperties;
    private final ObjectMapper objectMapper;
    private final ModelCallLimiter modelCallLimiter;
    private final ModelRoutingExecutor routingExecutor;
    private final ModelRoutingProperties properties;
    private final Map<String, RerankModelClient> configuredClientCache = new ConcurrentHashMap<>();

    @Override
    public RerankModelResponse rerank(RerankModelRequest request) {
        return routingExecutor.execute(ModelRouteRequest.<RerankModelResponse>builder()
                        .capability(ModelCapability.RERANK)
                        .purpose("RERANK")
                        .bizId(request == null || request.getQuery() == null ? null : "rerank-" + Integer.toHexString(request.getQuery().hashCode()))
                        .candidates(candidates(request))
                        .build())
                .getValue();
    }

    private List<ModelRouteCandidate<RerankModelResponse>> candidates(RerankModelRequest request) {
        List<ModelRoutingProperties.CandidateProperties> configuredCandidates = configuredCandidates();
        if (!configuredCandidates.isEmpty()) {
            return configuredCandidates.stream()
                    .map(candidate -> toConfiguredCandidate(candidate, request))
                    .toList();
        }
        List<ModelRouteCandidate<RerankModelResponse>> candidates = new ArrayList<>();
        BailianRerankModelClient realClient = realClientProvider.getIfAvailable();
        if (realClient != null) {
            candidates.add(ModelRouteCandidate.<RerankModelResponse>builder()
                    .candidateName("rerank-bailian")
                    .provider("bailian")
                    .modelName("configured-rerank-model")
                    .mockCandidate(false)
                    .supplier(() -> realClient.rerank(request))
                    .build());
        }
        MockRerankModelClient mockClient = mockClientProvider.getIfAvailable(MockRerankModelClient::new);
        if (realClient == null || properties.mockFallbackEnabled(ModelCapability.RERANK)) {
            candidates.add(ModelRouteCandidate.<RerankModelResponse>builder()
                    .candidateName("rerank-mock")
                    .provider("mock")
                    .modelName("mock-rerank")
                    .mockCandidate(true)
                    .supplier(() -> mockClient.rerank(request))
                    .build());
        }
        return candidates;
    }

    private List<ModelRoutingProperties.CandidateProperties> configuredCandidates() {
        return properties.routeOf(ModelCapability.RERANK).getCandidates().stream()
                .filter(ModelRoutingProperties.CandidateProperties::isEnabled)
                .toList();
    }

    private ModelRouteCandidate<RerankModelResponse> toConfiguredCandidate(ModelRoutingProperties.CandidateProperties candidate,
                                                                            RerankModelRequest request) {
        String provider = defaultText(candidate.getProvider(), "bailian");
        if ("mock".equalsIgnoreCase(provider)) {
            MockRerankModelClient mockClient = mockClientProvider.getIfAvailable(MockRerankModelClient::new);
            return ModelRouteCandidate.<RerankModelResponse>builder()
                    .candidateName(defaultText(candidate.getId(), "rerank-mock"))
                    .provider("mock")
                    .modelName(defaultText(candidate.getModelName(), "mock-rerank"))
                    .mockCandidate(true)
                    .supplier(() -> mockClient.rerank(request))
                    .build();
        }
        RerankModelProperties candidateProperties = copyProperties(candidate, provider);
        return ModelRouteCandidate.<RerankModelResponse>builder()
                .candidateName(defaultText(candidate.getId(), provider + "-" + candidateProperties.getModelName()))
                .provider(provider)
                .modelName(candidateProperties.getModelName())
                .mockCandidate(false)
                .supplier(() -> configuredRealClient(candidate, candidateProperties).rerank(request))
                .build();
    }

    private RerankModelClient configuredRealClient(ModelRoutingProperties.CandidateProperties candidate,
                                                   RerankModelProperties candidateProperties) {
        String cacheKey = defaultText(candidate.getId(), candidateProperties.getProvider() + ":" + candidateProperties.getModelName());
        return configuredClientCache.computeIfAbsent(cacheKey,
                key -> new BailianRerankModelClient(candidateProperties, objectMapper, modelCallLimiter));
    }

    private RerankModelProperties copyProperties(ModelRoutingProperties.CandidateProperties candidate, String provider) {
        RerankModelProperties copied = new RerankModelProperties();
        copied.setEnabled(true);
        copied.setMockEnabled(false);
        copied.setProvider(provider);
        copied.setBaseUrl(defaultText(candidate.getBaseUrl(), baseProperties.getBaseUrl()));
        copied.setApiKey(defaultText(candidate.getApiKey(), baseProperties.getApiKey()));
        copied.setModelName(defaultText(candidate.getModelName(), baseProperties.getModelName()));
        copied.setTopN(candidate.getTopN() == null ? baseProperties.getTopN() : candidate.getTopN());
        copied.setMaxCandidates(candidate.getMaxCandidates() == null ? baseProperties.getMaxCandidates() : candidate.getMaxCandidates());
        copied.setLogRequests(candidate.getLogRequests() == null ? baseProperties.isLogRequests() : candidate.getLogRequests());
        copied.setLogResponses(candidate.getLogResponses() == null ? baseProperties.isLogResponses() : candidate.getLogResponses());
        return copied;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
