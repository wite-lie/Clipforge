package com.clipforge.ai.infrastructure.routing.client;

import com.clipforge.ai.infrastructure.governance.ModelCallLimiter;
import com.clipforge.ai.infrastructure.output.ModelOutputTruncationDetector;
import com.clipforge.ai.api.routing.ModelCapability;
import com.clipforge.ai.api.routing.ModelRouteCandidate;
import com.clipforge.ai.api.routing.ModelRouteRequest;
import com.clipforge.ai.infrastructure.routing.ModelRoutingExecutor;
import com.clipforge.ai.infrastructure.routing.ModelRoutingProperties;
import com.clipforge.ai.infrastructure.vision.ImageUnderstandingJsonParser;
import com.clipforge.ai.infrastructure.vision.ImageUnderstandingPromptRenderer;
import com.clipforge.ai.infrastructure.vision.LangChain4jVisionModelClient;
import com.clipforge.ai.infrastructure.vision.MockVisionModelClient;
import com.clipforge.ai.api.vision.VisionModelClient;
import com.clipforge.ai.infrastructure.vision.VisionModelProperties;
import com.clipforge.ai.api.vision.VisionModelRequest;
import com.clipforge.ai.api.vision.VisionModelResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * RoutingVisionModelClient（路由视觉模型客户端）。
 *
 * <p>该类对外仍实现 VisionModelClient（视觉模型客户端），内部按配置在真实 VLM（视觉语言模型）和 Mock（模拟）
 * 降级候选之间做故障转移。
 */
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clipforge.model-routing", name = "enabled", havingValue = "true")
public class RoutingVisionModelClient implements VisionModelClient {

    private final ObjectProvider<LangChain4jVisionModelClient> realClientProvider;
    private final ObjectProvider<MockVisionModelClient> mockClientProvider;
    private final VisionModelProperties baseProperties;
    private final ImageUnderstandingPromptRenderer promptRenderer;
    private final ImageUnderstandingJsonParser jsonParser;
    private final ModelCallLimiter modelCallLimiter;
    private final ModelOutputTruncationDetector truncationDetector;
    private final ModelRoutingExecutor routingExecutor;
    private final ModelRoutingProperties properties;
    private final Map<String, VisionModelClient> configuredClientCache = new ConcurrentHashMap<>();

    @Override
    public VisionModelResponse understand(VisionModelRequest request) {
        return routingExecutor.execute(ModelRouteRequest.<VisionModelResponse>builder()
                        .capability(ModelCapability.VISION)
                        .purpose(request == null ? null : request.getPurpose())
                        .bizId(request == null ? null : request.getRunId())
                        .traceRunId(request == null ? null : request.getRunId())
                        .candidates(candidates(request))
                        .build())
                .getValue();
    }

    private List<ModelRouteCandidate<VisionModelResponse>> candidates(VisionModelRequest request) {
        List<ModelRoutingProperties.CandidateProperties> configuredCandidates = configuredCandidates();
        if (!configuredCandidates.isEmpty()) {
            return configuredCandidates.stream()
                    .map(candidate -> toConfiguredCandidate(candidate, request))
                    .toList();
        }
        List<ModelRouteCandidate<VisionModelResponse>> candidates = new ArrayList<>();
        LangChain4jVisionModelClient realClient = realClientProvider.getIfAvailable();
        if (realClient != null) {
            candidates.add(ModelRouteCandidate.<VisionModelResponse>builder()
                    .candidateName("vision-langchain4j")
                    .provider("langchain4j")
                    .modelName("configured-vision-model")
                    .mockCandidate(false)
                    .supplier(() -> realClient.understand(request))
                    .build());
        }
        MockVisionModelClient mockClient = mockClientProvider.getIfAvailable(MockVisionModelClient::new);
        if (realClient == null || properties.mockFallbackEnabled(ModelCapability.VISION)) {
            candidates.add(ModelRouteCandidate.<VisionModelResponse>builder()
                    .candidateName("vision-mock")
                    .provider("mock")
                    .modelName("mock-vision-model")
                    .mockCandidate(true)
                    .supplier(() -> mockClient.understand(request))
                    .build());
        }
        return candidates;
    }

    private List<ModelRoutingProperties.CandidateProperties> configuredCandidates() {
        return properties.routeOf(ModelCapability.VISION).getCandidates().stream()
                .filter(ModelRoutingProperties.CandidateProperties::isEnabled)
                .toList();
    }

    private ModelRouteCandidate<VisionModelResponse> toConfiguredCandidate(ModelRoutingProperties.CandidateProperties candidate,
                                                                            VisionModelRequest request) {
        String provider = defaultText(candidate.getProvider(), "openai-compatible");
        if ("mock".equalsIgnoreCase(provider)) {
            MockVisionModelClient mockClient = mockClientProvider.getIfAvailable(MockVisionModelClient::new);
            return ModelRouteCandidate.<VisionModelResponse>builder()
                    .candidateName(defaultText(candidate.getId(), "vision-mock"))
                    .provider("mock")
                    .modelName(defaultText(candidate.getModelName(), "mock-vision-model"))
                    .mockCandidate(true)
                    .supplier(() -> mockClient.understand(request))
                    .build();
        }
        VisionModelProperties candidateProperties = copyProperties(candidate, provider);
        return ModelRouteCandidate.<VisionModelResponse>builder()
                .candidateName(defaultText(candidate.getId(), provider + "-" + candidateProperties.getModelName()))
                .provider(provider)
                .modelName(candidateProperties.getModelName())
                .mockCandidate(false)
                .supplier(() -> configuredRealClient(candidate, candidateProperties).understand(request))
                .build();
    }

    private VisionModelClient configuredRealClient(ModelRoutingProperties.CandidateProperties candidate, VisionModelProperties candidateProperties) {
        String cacheKey = defaultText(candidate.getId(), candidateProperties.getProvider() + ":" + candidateProperties.getModelName());
        return configuredClientCache.computeIfAbsent(cacheKey,
                key -> new LangChain4jVisionModelClient(candidateProperties, promptRenderer, jsonParser, modelCallLimiter, truncationDetector));
    }

    private VisionModelProperties copyProperties(ModelRoutingProperties.CandidateProperties candidate, String provider) {
        VisionModelProperties copied = new VisionModelProperties();
        copied.setMockEnabled(false);
        copied.setProvider(provider);
        copied.setBaseUrl(defaultText(candidate.getBaseUrl(), baseProperties.getBaseUrl()));
        copied.setApiKey(defaultText(candidate.getApiKey(), baseProperties.getApiKey()));
        copied.setModelName(defaultText(candidate.getModelName(), baseProperties.getModelName()));
        copied.setTemperature(candidate.getTemperature() == null ? baseProperties.getTemperature() : candidate.getTemperature());
        copied.setMaxTokens(candidate.getMaxTokens() == null ? baseProperties.getMaxTokens() : candidate.getMaxTokens());
        copied.setLogRequests(candidate.getLogRequests() == null ? baseProperties.isLogRequests() : candidate.getLogRequests());
        copied.setLogResponses(candidate.getLogResponses() == null ? baseProperties.isLogResponses() : candidate.getLogResponses());
        return copied;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
