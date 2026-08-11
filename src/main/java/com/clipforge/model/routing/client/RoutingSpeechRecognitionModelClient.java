package com.clipforge.model.routing.client;

import com.clipforge.model.routing.ModelCapability;
import com.clipforge.model.routing.ModelRouteCandidate;
import com.clipforge.model.routing.ModelRouteRequest;
import com.clipforge.model.routing.ModelRoutingExecutor;
import com.clipforge.model.routing.ModelRoutingProperties;
import com.clipforge.model.speech.BailianSpeechRecognitionModelClient;
import com.clipforge.model.speech.MockSpeechRecognitionModelClient;
import com.clipforge.model.speech.SpeechRecognitionModelClient;
import com.clipforge.model.speech.SpeechRecognitionModelRequest;
import com.clipforge.model.speech.SpeechRecognitionModelResponse;
import com.clipforge.model.speech.SpeechRecognitionProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * RoutingSpeechRecognitionModelClient（路由语音识别模型客户端）。
 *
 * <p>该类对外仍实现 SpeechRecognitionModelClient（语音识别模型客户端），内部按配置在百炼 ASR（自动语音识别）
 * 和 Mock（模拟）降级候选之间做故障转移。
 */
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clipforge.model-routing", name = "enabled", havingValue = "true")
public class RoutingSpeechRecognitionModelClient implements SpeechRecognitionModelClient {

    private final ObjectProvider<BailianSpeechRecognitionModelClient> realClientProvider;
    private final ObjectProvider<MockSpeechRecognitionModelClient> mockClientProvider;
    private final SpeechRecognitionProperties baseProperties;
    private final ObjectMapper objectMapper;
    private final ModelRoutingExecutor routingExecutor;
    private final ModelRoutingProperties properties;
    private final Map<String, SpeechRecognitionModelClient> configuredClientCache = new ConcurrentHashMap<>();

    @Override
    public SpeechRecognitionModelResponse transcribe(SpeechRecognitionModelRequest request) {
        return routingExecutor.execute(ModelRouteRequest.<SpeechRecognitionModelResponse>builder()
                        .capability(ModelCapability.SPEECH)
                        .purpose("SPEECH_ASR")
                        .bizId(request == null || request.getAudioPath() == null ? null : request.getAudioPath().toString())
                        .candidates(candidates(request))
                        .build())
                .getValue();
    }

    private List<ModelRouteCandidate<SpeechRecognitionModelResponse>> candidates(SpeechRecognitionModelRequest request) {
        List<ModelRoutingProperties.CandidateProperties> configuredCandidates = configuredCandidates();
        if (!configuredCandidates.isEmpty()) {
            return configuredCandidates.stream()
                    .map(candidate -> toConfiguredCandidate(candidate, request))
                    .toList();
        }
        List<ModelRouteCandidate<SpeechRecognitionModelResponse>> candidates = new ArrayList<>();
        BailianSpeechRecognitionModelClient realClient = realClientProvider.getIfAvailable();
        if (realClient != null) {
            candidates.add(ModelRouteCandidate.<SpeechRecognitionModelResponse>builder()
                    .candidateName("speech-bailian")
                    .provider("bailian")
                    .modelName("configured-asr-model")
                    .mockCandidate(false)
                    .supplier(() -> realClient.transcribe(request))
                    .build());
        }
        MockSpeechRecognitionModelClient mockClient = mockClientProvider.getIfAvailable(MockSpeechRecognitionModelClient::new);
        if (realClient == null || properties.mockFallbackEnabled(ModelCapability.SPEECH)) {
            candidates.add(ModelRouteCandidate.<SpeechRecognitionModelResponse>builder()
                    .candidateName("speech-mock")
                    .provider("mock")
                    .modelName("mock-speech-recognition-model")
                    .mockCandidate(true)
                    .supplier(() -> mockClient.transcribe(request))
                    .build());
        }
        return candidates;
    }

    private List<ModelRoutingProperties.CandidateProperties> configuredCandidates() {
        return properties.routeOf(ModelCapability.SPEECH).getCandidates().stream()
                .filter(ModelRoutingProperties.CandidateProperties::isEnabled)
                .toList();
    }

    private ModelRouteCandidate<SpeechRecognitionModelResponse> toConfiguredCandidate(ModelRoutingProperties.CandidateProperties candidate,
                                                                                       SpeechRecognitionModelRequest request) {
        String provider = defaultText(candidate.getProvider(), "bailian");
        if ("mock".equalsIgnoreCase(provider)) {
            MockSpeechRecognitionModelClient mockClient = mockClientProvider.getIfAvailable(MockSpeechRecognitionModelClient::new);
            return ModelRouteCandidate.<SpeechRecognitionModelResponse>builder()
                    .candidateName(defaultText(candidate.getId(), "speech-mock"))
                    .provider("mock")
                    .modelName(defaultText(candidate.getModelName(), "mock-speech-recognition-model"))
                    .mockCandidate(true)
                    .supplier(() -> mockClient.transcribe(request))
                    .build();
        }
        SpeechRecognitionProperties candidateProperties = copyProperties(candidate, provider);
        return ModelRouteCandidate.<SpeechRecognitionModelResponse>builder()
                .candidateName(defaultText(candidate.getId(), provider + "-" + candidateProperties.getModelName()))
                .provider(provider)
                .modelName(candidateProperties.getModelName())
                .mockCandidate(false)
                .supplier(() -> configuredRealClient(candidate, candidateProperties).transcribe(request))
                .build();
    }

    private SpeechRecognitionModelClient configuredRealClient(ModelRoutingProperties.CandidateProperties candidate,
                                                              SpeechRecognitionProperties candidateProperties) {
        String cacheKey = defaultText(candidate.getId(), candidateProperties.getProvider() + ":" + candidateProperties.getModelName());
        return configuredClientCache.computeIfAbsent(cacheKey,
                key -> new BailianSpeechRecognitionModelClient(candidateProperties, objectMapper));
    }

    private SpeechRecognitionProperties copyProperties(ModelRoutingProperties.CandidateProperties candidate, String provider) {
        SpeechRecognitionProperties copied = new SpeechRecognitionProperties();
        copied.setMockEnabled(false);
        copied.setProvider(provider);
        copied.setBaseUrl(defaultText(candidate.getBaseUrl(), baseProperties.getBaseUrl()));
        copied.setApiKey(defaultText(candidate.getApiKey(), baseProperties.getApiKey()));
        copied.setModelName(defaultText(candidate.getModelName(), baseProperties.getModelName()));
        copied.setTimeoutSeconds(candidate.getTimeoutSeconds() == null ? baseProperties.getTimeoutSeconds() : candidate.getTimeoutSeconds());
        copied.setAudioFormat(defaultText(candidate.getAudioFormat(), baseProperties.getAudioFormat()));
        copied.setSampleRate(candidate.getSampleRate() == null ? baseProperties.getSampleRate() : candidate.getSampleRate());
        copied.setLogRequests(candidate.getLogRequests() == null ? baseProperties.isLogRequests() : candidate.getLogRequests());
        copied.setLogResponses(candidate.getLogResponses() == null ? baseProperties.isLogResponses() : candidate.getLogResponses());
        return copied;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
