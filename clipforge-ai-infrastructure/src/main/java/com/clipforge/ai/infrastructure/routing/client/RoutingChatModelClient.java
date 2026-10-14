package com.clipforge.ai.infrastructure.routing.client;

import com.clipforge.ai.api.chat.ChatModelClient;
import com.clipforge.ai.infrastructure.chat.ChatModelProperties;
import com.clipforge.ai.api.chat.ChatModelRequest;
import com.clipforge.ai.api.chat.ChatModelResponse;
import com.clipforge.ai.infrastructure.chat.LangChain4jChatModelClient;
import com.clipforge.ai.infrastructure.chat.MockChatModelClient;
import com.clipforge.ai.infrastructure.governance.ModelCallLimiter;
import com.clipforge.ai.infrastructure.output.ModelOutputTruncationDetector;
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

/**
 * RoutingChatModelClient（路由聊天模型客户端）。
 *
 * <p>该类对外仍实现 ChatModelClient（聊天模型客户端），内部按配置在真实 LLM（大语言模型）和 Mock（模拟）
 * 降级候选之间做故障转移。
 */
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clipforge.model-routing", name = "enabled", havingValue = "true")
public class RoutingChatModelClient implements ChatModelClient {

    private final ObjectProvider<LangChain4jChatModelClient> realClientProvider;
    private final ObjectProvider<MockChatModelClient> mockClientProvider;
    private final ChatModelProperties baseProperties;
    private final ModelCallLimiter modelCallLimiter;
    private final ModelOutputTruncationDetector truncationDetector;
    private final ModelRoutingExecutor routingExecutor;
    private final ModelRoutingProperties properties;
    private final Map<String, ChatModelClient> configuredClientCache = new ConcurrentHashMap<>();

    @Override
    public ChatModelResponse chat(ChatModelRequest request) {
        return routingExecutor.execute(ModelRouteRequest.<ChatModelResponse>builder()
                        .capability(ModelCapability.CHAT)
                        .purpose(request == null ? null : request.getPurpose())
                        .bizId(request == null ? null : request.getRunId())
                        .traceRunId(request == null ? null : request.getRunId())
                        .candidates(candidates(request))
                        .build())
                .getValue();
    }

    private List<ModelRouteCandidate<ChatModelResponse>> candidates(ChatModelRequest request) {
        List<ModelRoutingProperties.CandidateProperties> configuredCandidates = configuredCandidates();
        if (!configuredCandidates.isEmpty()) {
            return configuredCandidates.stream()
                    .map(candidate -> toConfiguredCandidate(candidate, request))
                    .toList();
        }
        List<ModelRouteCandidate<ChatModelResponse>> candidates = new ArrayList<>();
        LangChain4jChatModelClient realClient = realClientProvider.getIfAvailable();
        if (realClient != null) {
            candidates.add(ModelRouteCandidate.<ChatModelResponse>builder()
                    .candidateName("chat-langchain4j")
                    .provider("langchain4j")
                    .modelName("configured-chat-model")
                    .mockCandidate(false)
                    .supplier(() -> realClient.chat(request))
                    .build());
        }
        MockChatModelClient mockClient = mockClientProvider.getIfAvailable(MockChatModelClient::new);
        if (realClient == null || properties.mockFallbackEnabled(ModelCapability.CHAT)) {
            candidates.add(ModelRouteCandidate.<ChatModelResponse>builder()
                    .candidateName("chat-mock")
                    .provider("mock")
                    .modelName("mock-chat-model")
                    .mockCandidate(true)
                    .supplier(() -> mockClient.chat(request))
                    .build());
        }
        return candidates;
    }

    private List<ModelRoutingProperties.CandidateProperties> configuredCandidates() {
        return properties.routeOf(ModelCapability.CHAT).getCandidates().stream()
                .filter(ModelRoutingProperties.CandidateProperties::isEnabled)
                .toList();
    }

    private ModelRouteCandidate<ChatModelResponse> toConfiguredCandidate(ModelRoutingProperties.CandidateProperties candidate,
                                                                          ChatModelRequest request) {
        String provider = defaultText(candidate.getProvider(), "openai-compatible");
        if ("mock".equalsIgnoreCase(provider)) {
            MockChatModelClient mockClient = mockClientProvider.getIfAvailable(MockChatModelClient::new);
            return ModelRouteCandidate.<ChatModelResponse>builder()
                    .candidateName(defaultText(candidate.getId(), "chat-mock"))
                    .provider("mock")
                    .modelName(defaultText(candidate.getModelName(), "mock-chat-model"))
                    .mockCandidate(true)
                    .supplier(() -> mockClient.chat(request))
                    .build();
        }
        ChatModelProperties candidateProperties = copyProperties(candidate, provider);
        return ModelRouteCandidate.<ChatModelResponse>builder()
                .candidateName(defaultText(candidate.getId(), provider + "-" + candidateProperties.getModelName()))
                .provider(provider)
                .modelName(candidateProperties.getModelName())
                .mockCandidate(false)
                .supplier(() -> configuredRealClient(candidate, candidateProperties).chat(request))
                .build();
    }

    private ChatModelClient configuredRealClient(ModelRoutingProperties.CandidateProperties candidate, ChatModelProperties candidateProperties) {
        String cacheKey = defaultText(candidate.getId(), candidateProperties.getProvider() + ":" + candidateProperties.getModelName());
        return configuredClientCache.computeIfAbsent(cacheKey,
                key -> new LangChain4jChatModelClient(candidateProperties, modelCallLimiter, truncationDetector));
    }

    private ChatModelProperties copyProperties(ModelRoutingProperties.CandidateProperties candidate, String provider) {
        ChatModelProperties copied = new ChatModelProperties();
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
