package com.clipforge.model.speech;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * BailianSpeechRecognitionModelClient（百炼语音识别模型客户端）。
 *
 * <p>通过 DashScope（百炼模型服务）的 ASR（自动语音识别）HTTP（超文本传输协议）接口转写本地音频文件。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.speech", name = "mock-enabled", havingValue = "false")
public class BailianSpeechRecognitionModelClient implements SpeechRecognitionModelClient {

    private final SpeechRecognitionProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final BailianSpeechRecognitionResponseParser responseParser;

    public BailianSpeechRecognitionModelClient(SpeechRecognitionProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
        this.responseParser = new BailianSpeechRecognitionResponseParser(objectMapper);
    }

    @Override
    public SpeechRecognitionModelResponse transcribe(SpeechRecognitionModelRequest request) {
        validate(request);
        try {
            Map<String, Object> requestBody = buildRequestBody(request);
            String requestJson = objectMapper.writeValueAsString(requestBody);
            printRequestSummary(request);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl()))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("X-DashScope-SSE", "disable")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.MODEL_CALL_ERROR,
                        "百炼 ASR API 调用失败，HTTP 状态码：" + response.statusCode()
                                + "，响应摘要：" + compact(response.body(), 500));
            }
            SpeechRecognitionModelResponse parsed = responseParser.parse(response.body(), properties.getModelName());
            printResponseSummary(response.statusCode(), response.body(), parsed);
            return parsed;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "百炼 ASR API 调用失败：" + exception.getMessage(), exception);
        }
    }

    private Map<String, Object> buildRequestBody(SpeechRecognitionModelRequest request) throws Exception {
        Map<String, Object> inputAudio = new LinkedHashMap<>();
        inputAudio.put("data", audioData(request));

        Map<String, Object> contentItem = new LinkedHashMap<>();
        contentItem.put("type", "input_audio");
        contentItem.put("input_audio", inputAudio);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", List.of(contentItem));

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("messages", List.of(message));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("format", properties.getAudioFormat());
        parameters.put("sample_rate", String.valueOf(properties.getSampleRate()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModelName());
        body.put("input", input);
        body.put("parameters", parameters);
        return body;
    }

    private String audioData(SpeechRecognitionModelRequest request) throws Exception {
        byte[] bytes = Files.readAllBytes(request.getAudioPath());
        String encoded = Base64.getEncoder().encodeToString(bytes);
        return "data:" + audioMimeType() + ";base64," + encoded;
    }

    private String audioMimeType() {
        String format = properties.getAudioFormat();
        if (format == null || format.isBlank()) {
            return "audio/wav";
        }
        return switch (format.toLowerCase()) {
            case "mp3", "mpeg" -> "audio/mpeg";
            case "m4a" -> "audio/mp4";
            case "ogg" -> "audio/ogg";
            case "flac" -> "audio/flac";
            default -> "audio/wav";
        };
    }

    private void printRequestSummary(SpeechRecognitionModelRequest request) throws Exception {
        if (!properties.isLogRequests()) {
            return;
        }
        System.out.println("百炼 ASR 请求摘要：model=" + properties.getModelName()
                + "，audioPath=" + request.getAudioPath()
                + "，audioBytes=" + Files.size(request.getAudioPath())
                + "，format=" + properties.getAudioFormat()
                + "，sampleRate=" + properties.getSampleRate()
                + "，audioData=<base64 已省略>");
    }

    private void printResponseSummary(int statusCode, String rawBody, SpeechRecognitionModelResponse parsed) {
        if (!properties.isLogResponses()) {
            return;
        }
        System.out.println("百炼 ASR 响应摘要：httpStatus=" + statusCode
                + "，rawLength=" + (rawBody == null ? 0 : rawBody.length())
                + "，speechDetected=" + parsed.getSpeechDetected()
                + "，segmentCount=" + (parsed.getSegments() == null ? 0 : parsed.getSegments().size())
                + "，transcript=" + compact(parsed.getTranscriptText(), 300));
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

    private void validate(SpeechRecognitionModelRequest request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "缺少百炼 ASR API Key，请配置 clipforge.speech.api-key");
        }
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "缺少百炼 ASR base-url，请配置 clipforge.speech.base-url");
        }
        if (properties.getModelName() == null || properties.getModelName().isBlank()) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_ERROR, "缺少百炼 ASR model-name，请配置 clipforge.speech.model-name");
        }
        if (request == null || request.getAudioPath() == null || !Files.exists(request.getAudioPath())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "待识别音频文件不存在：" + (request == null ? null : request.getAudioPath()));
        }
    }
}
