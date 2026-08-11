package com.clipforge.model.routing.client;

import com.clipforge.model.embedding.EmbeddingModelClient;
import com.clipforge.model.embedding.EmbeddingModelProperties;
import com.clipforge.model.embedding.EmbeddingModelRequest;
import com.clipforge.model.embedding.EmbeddingModelResponse;
import com.clipforge.model.embedding.LangChain4jEmbeddingModelClient;
import com.clipforge.model.embedding.MockEmbeddingModelClient;
import com.clipforge.model.governance.ModelCallLimiter;
import com.clipforge.model.routing.ModelCapability;
import com.clipforge.model.routing.ModelRouteCandidate;
import com.clipforge.model.routing.ModelRouteRequest;
import com.clipforge.model.routing.ModelRoutingExecutor;
import com.clipforge.model.routing.ModelRoutingProperties;
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
 * RoutingEmbeddingModelClient（路由嵌入模型客户端）。
 *
 * <p>该类对外仍实现 EmbeddingModelClient（嵌入模型客户端），内部按配置在真实向量化模型和 Mock（模拟）
 * 降级候选之间做故障转移。
 */
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clipforge.model-routing", name = "enabled", havingValue = "true")
public class RoutingEmbeddingModelClient implements EmbeddingModelClient {

    private final ObjectProvider<LangChain4jEmbeddingModelClient> realClientProvider;
    private final ObjectProvider<MockEmbeddingModelClient> mockClientProvider;
    private final EmbeddingModelProperties embeddingProperties;
    private final ModelCallLimiter modelCallLimiter;
    private final ModelRoutingExecutor routingExecutor;
    private final ModelRoutingProperties properties;
    private final Map<String, EmbeddingModelClient> configuredClientCache = new ConcurrentHashMap<>();

    @Override
    public EmbeddingModelResponse embed(EmbeddingModelRequest request) {
        return routingExecutor.execute(ModelRouteRequest.<EmbeddingModelResponse>builder()
                        .capability(ModelCapability.EMBEDDING)
                        .purpose("EMBEDDING")
                        .bizId("embedding-batch")
                        .candidates(candidates(request))
                        .build())
                .getValue();
    }

    private List<ModelRouteCandidate<EmbeddingModelResponse>> candidates(EmbeddingModelRequest request) {
        List<ModelRoutingProperties.CandidateProperties> configuredCandidates = configuredCandidates();
        if (!configuredCandidates.isEmpty()) {
            return configuredCandidates.stream()
                    .map(candidate -> toConfiguredCandidate(candidate, request))
                    .toList();
        }
        List<ModelRouteCandidate<EmbeddingModelResponse>> candidates = new ArrayList<>();
        LangChain4jEmbeddingModelClient realClient = realClientProvider.getIfAvailable();
        if (realClient != null) {
            candidates.add(ModelRouteCandidate.<EmbeddingModelResponse>builder()
                    .candidateName("embedding-langchain4j")
                    .provider("langchain4j")
                    .modelName("configured-embedding-model")
                    .mockCandidate(false)
                    .supplier(() -> realClient.embed(request))
                    .build());
        }
        MockEmbeddingModelClient mockClient = mockClientProvider.getIfAvailable(() -> new MockEmbeddingModelClient(embeddingProperties));
        if (realClient == null || properties.mockFallbackEnabled(ModelCapability.EMBEDDING)) {
            candidates.add(ModelRouteCandidate.<EmbeddingModelResponse>builder()
                    .candidateName("embedding-mock")
                    .provider("mock")
                    .modelName("mock-embedding-model")
                    .mockCandidate(true)
                    .supplier(() -> mockClient.embed(request))
                    .build());
        }
        return candidates;
    }

    private List<ModelRoutingProperties.CandidateProperties> configuredCandidates() {
        return properties.routeOf(ModelCapability.EMBEDDING).getCandidates().stream()
                .filter(ModelRoutingProperties.CandidateProperties::isEnabled)
                .toList();
    }

    private ModelRouteCandidate<EmbeddingModelResponse> toConfiguredCandidate(ModelRoutingProperties.CandidateProperties candidate,
                                                                               EmbeddingModelRequest request) {
        String provider = defaultText(candidate.getProvider(), "openai-compatible");
        if ("mock".equalsIgnoreCase(provider)) {
            MockEmbeddingModelClient mockClient = mockClientProvider.getIfAvailable(() -> new MockEmbeddingModelClient(embeddingProperties));
            return ModelRouteCandidate.<EmbeddingModelResponse>builder()
                    .candidateName(defaultText(candidate.getId(), "embedding-mock"))
                    .provider("mock")
                    .modelName(defaultText(candidate.getModelName(), "mock-embedding-model"))
                    .mockCandidate(true)
                    .supplier(() -> mockClient.embed(request))
                    .build();
        }
        EmbeddingModelProperties candidateProperties = copyProperties(candidate, provider);
        return ModelRouteCandidate.<EmbeddingModelResponse>builder()
                .candidateName(defaultText(candidate.getId(), provider + "-" + candidateProperties.getModelName()))
                .provider(provider)
                .modelName(candidateProperties.getModelName())
                .mockCandidate(false)
                .supplier(() -> configuredRealClient(candidate, candidateProperties).embed(request))
                .build();
    }

    private EmbeddingModelClient configuredRealClient(ModelRoutingProperties.CandidateProperties candidate,
                                                      EmbeddingModelProperties candidateProperties) {
        String cacheKey = defaultText(candidate.getId(), candidateProperties.getProvider() + ":" + candidateProperties.getModelName());
        return configuredClientCache.computeIfAbsent(cacheKey,
                key -> new LangChain4jEmbeddingModelClient(candidateProperties, modelCallLimiter));
    }

    private EmbeddingModelProperties copyProperties(ModelRoutingProperties.CandidateProperties candidate, String provider) {
        EmbeddingModelProperties copied = new EmbeddingModelProperties();
        copied.setMockEnabled(false);
        copied.setProvider(provider);
        copied.setBaseUrl(defaultText(candidate.getBaseUrl(), embeddingProperties.getBaseUrl()));
        copied.setApiKey(defaultText(candidate.getApiKey(), embeddingProperties.getApiKey()));
        copied.setModelName(defaultText(candidate.getModelName(), embeddingProperties.getModelName()));
        copied.setDimension(candidate.getDimension() == null ? embeddingProperties.getDimension() : candidate.getDimension());
        copied.setMaxSegmentsPerBatch(candidate.getMaxSegmentsPerBatch() == null
                ? embeddingProperties.getMaxSegmentsPerBatch()
                : candidate.getMaxSegmentsPerBatch());
        copied.setLogRequests(candidate.getLogRequests() == null ? embeddingProperties.isLogRequests() : candidate.getLogRequests());
        copied.setLogResponses(candidate.getLogResponses() == null ? embeddingProperties.isLogResponses() : candidate.getLogResponses());
        return copied;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
