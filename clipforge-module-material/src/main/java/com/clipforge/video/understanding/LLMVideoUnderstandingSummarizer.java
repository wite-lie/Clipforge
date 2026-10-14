package com.clipforge.video.understanding;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.ai.api.chat.ChatMessage;
import com.clipforge.ai.api.chat.ChatModelClient;
import com.clipforge.ai.infrastructure.chat.ChatModelProperties;
import com.clipforge.ai.api.chat.ChatModelRequest;
import com.clipforge.ai.api.chat.ChatModelResponse;
import com.clipforge.material.video.understanding.VideoUnderstandingResult;
import com.clipforge.material.video.understanding.VideoUnderstandingStatus;
import com.clipforge.observability.usage.ModelCallRecord;
import com.clipforge.observability.usage.ModelUsageRecorder;
import com.clipforge.material.video.understanding.highlight.VideoHighlightCandidate;
import com.clipforge.video.understanding.highlight.VideoHighlightRecommendRequest;
import com.clipforge.video.understanding.highlight.VideoHighlightRecommendResult;
import com.clipforge.video.understanding.highlight.VideoHighlightRecommender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LLMVideoUnderstandingSummarizer（大语言模型视频理解摘要器）。
 *
 * <p>该实现调用 ChatModelClient（聊天模型客户端），把视频事实包综合为结构化 VideoUnderstandingResult（视频理解结果）。
 */
@Component
@RequiredArgsConstructor
public class LLMVideoUnderstandingSummarizer {

    /** PURPOSE（调用用途）用于模型用量统计和后续成本复盘。 */
    public static final String PURPOSE = "VIDEO_UNDERSTANDING_SUMMARY";

    /** promptRenderer（提示词渲染器）负责从 Prompt（提示词）文件构造模型输入。 */
    private final VideoUnderstandingPromptRenderer promptRenderer;

    /** chatModelClient（聊天模型客户端）用于调用真实或 Mock（模拟）LLM。 */
    private final ChatModelClient chatModelClient;

    /** chatModelProperties（聊天模型配置）用于失败记录时补齐供应商和模型名称。 */
    private final ChatModelProperties chatModelProperties;

    /** modelUsageRecorder（模型用量记录器）用于保存 Token（词元）和耗时。 */
    private final ModelUsageRecorder modelUsageRecorder;

    /** properties（配置）用于控制温度、最大输出和提示词版本。 */
    private final VideoUnderstandingSummaryProperties properties;

    /** objectMapper（JSON 对象映射器）用于解析模型返回。 */
    private final ObjectMapper objectMapper;

    /** highlightRecommender（高光推荐器）用于补充结构化高光片段，避免模型凭空编造时间段。 */
    private final VideoHighlightRecommender highlightRecommender;

    /**
     * summarize（摘要）用于调用 LLM 生成视频理解结果。
     */
    public VideoUnderstandingResult summarize(VideoUnderstandingContext context) {
        RenderedVideoUnderstandingPrompt prompt = promptRenderer.render(context);
        printPromptIfEnabled(prompt);
        ChatModelRequest request = ChatModelRequest.builder()
                .runId(context.getRunId())
                .purpose(PURPOSE)
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxOutputTokens())
                .messages(List.of(
                        ChatMessage.system(prompt.getSystemPrompt()),
                        ChatMessage.user(prompt.getUserPrompt())
                ))
                .build();
        long startedAt = System.currentTimeMillis();
        ChatModelResponse response = null;
        try {
            response = chatModelClient.chat(request);
            printResponseIfEnabled(response);
            VideoUnderstandingResult result = parse(response == null ? null : response.getContent());
            VideoUnderstandingResult normalizedResult = normalizeResult(context, response, result);
            recordUsage(context, response, System.currentTimeMillis() - startedAt, true, null);
            return normalizedResult;
        } catch (RuntimeException exception) {
            recordUsage(context, response, System.currentTimeMillis() - startedAt, false, exception);
            throw exception;
        }
    }

    /**
     * parse（解析）用于把模型返回 JSON（对象表示格式）转换为视频理解结果。
     */
    private VideoUnderstandingResult parse(String rawContent) {
        String json = extractJsonObject(rawContent);
        try {
            return objectMapper.readValue(json, VideoUnderstandingResult.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "视频理解摘要模型输出不是合法 JSON：" + exception.getOriginalMessage(), exception);
        }
    }

    /**
     * normalizeResult（规范化结果）用于补齐模型不应自行决定的运行字段和摘要元信息。
     */
    private VideoUnderstandingResult normalizeResult(VideoUnderstandingContext context, ChatModelResponse response, VideoUnderstandingResult result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "视频理解摘要模型结果为空");
        }
        result.setRunId(context.getRunId());
        result.setMaterialId(context.getMaterialId());
        result.setStatus(hasText(result.getStatus()) ? result.getStatus() : VideoUnderstandingStatus.SUCCEEDED.getValue());
        result.setKeyVisualMoments(nonNullList(result.getKeyVisualMoments()));
        result.setHighlightCandidates(nonNullList(result.getHighlightCandidates()));
        result.setStructuredHighlightCandidates(recommendStructuredHighlights(context));
        if (result.getHighlightCandidates().isEmpty()) {
            result.setHighlightCandidates(formatHighlightCandidates(result.getStructuredHighlightCandidates()));
        }
        result.setCreativeAngles(nonNullList(result.getCreativeAngles()));
        result.setRiskNotes(nonNullList(result.getRiskNotes()));
        if (!hasText(result.getSummaryForCreation())) {
            result.setSummaryForCreation(buildSummaryForCreation(context, result));
        }
        if (!VideoUnderstandingStatus.SUCCEEDED.getValue().equals(result.getStatus())
                && !VideoUnderstandingStatus.PARTIAL_SUCCEEDED.getValue().equals(result.getStatus())) {
            result.setStatus(VideoUnderstandingStatus.PARTIAL_SUCCEEDED.getValue());
            result.getRiskNotes().add("视频理解摘要模型返回了非标准状态，系统已规范化为 PARTIAL_SUCCEEDED。");
        }
        result.setSummaryStrategy("LLM");
        result.setSummaryModelName(response == null ? chatModelProperties.getModelName() : response.getModelName());
        result.setSummaryPromptVersion(properties.getPromptVersion());
        return result;
    }

    /**
     * buildSummaryForCreation（构建创作摘要）用于模型漏填 summaryForCreation 时兜底生成可注入文本。
     */
    private String buildSummaryForCreation(VideoUnderstandingContext context, VideoUnderstandingResult result) {
        return "【视频理解摘要】\n"
                + "视频素材编号：" + context.getMaterialId() + "\n"
                + "整体主题：" + safe(result.getOverallTheme()) + "\n"
                + "视觉摘要：" + safe(result.getVisualSummary()) + "\n"
                + "音频摘要：" + safe(result.getAudioSummary()) + "\n"
                + "可能高光片段：" + result.getHighlightCandidates() + "\n"
                + "适合创作的卖点：" + result.getCreativeAngles() + "\n"
                + "风险提示：" + result.getRiskNotes();
    }

    /**
     * recommendStructuredHighlights（推荐结构化高光）用于把本地时间线分析补入 LLM 摘要结果。
     */
    private List<VideoHighlightCandidate> recommendStructuredHighlights(VideoUnderstandingContext context) {
        VideoHighlightRecommendResult result = highlightRecommender.recommend(VideoHighlightRecommendRequest.builder()
                .context(context)
                .build());
        return result == null || result.getCandidates() == null ? List.of() : result.getCandidates();
    }

    /**
     * formatHighlightCandidates（格式化高光候选）用于兼容旧字符串字段。
     */
    private List<String> formatHighlightCandidates(List<VideoHighlightCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream()
                .map(candidate -> formatSeconds(candidate.getStartSeconds()) + "-" + formatSeconds(candidate.getEndSeconds())
                        + " 秒：" + safe(candidate.getReason()) + "置信度：" + String.format("%.2f", candidate.getConfidence() == null ? 0D : candidate.getConfidence()))
                .toList();
    }

    /**
     * recordUsage（记录用量）用于将视频摘要模型调用纳入现有成本统计。
     */
    private void recordUsage(VideoUnderstandingContext context, ChatModelResponse response, long latencyMs, boolean success, RuntimeException exception) {
        modelUsageRecorder.record(ModelCallRecord.builder()
                .runId(context == null ? null : context.getRunId())
                .provider(response == null ? chatModelProperties.getProvider() : response.getProvider())
                .modelName(response == null ? chatModelProperties.getModelName() : response.getModelName())
                .purpose(PURPOSE)
                .promptTokens(response == null ? null : response.getPromptTokens())
                .completionTokens(response == null ? null : response.getCompletionTokens())
                .totalTokens(response == null ? null : response.getTotalTokens())
                .latencyMs(latencyMs)
                .success(success)
                .errorCode(exception == null ? null : exception.getClass().getSimpleName())
                .errorMessage(exception == null ? null : exception.getMessage())
                .build());
    }

    /**
     * extractJsonObject（提取 JSON 对象）用于兼容模型返回 Markdown 代码块或前后解释文字。
     */
    private String extractJsonObject(String rawContent) {
        if (!hasText(rawContent)) {
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "视频理解摘要模型输出为空");
        }
        String content = rawContent.strip()
                .replaceFirst("^```json\\s*", "")
                .replaceFirst("^```\\s*", "")
                .replaceFirst("\\s*```$", "");
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "视频理解摘要模型输出中没有完整 JSON 对象");
        }
        return content.substring(start, end + 1);
    }

    private List<String> nonNullList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatSeconds(Double value) {
        return String.format("%.1f", value == null ? 0D : value);
    }

    private void printPromptIfEnabled(RenderedVideoUnderstandingPrompt prompt) {
        if (!properties.isLogPrompt()) {
            return;
        }
        System.out.println("视频理解 LLM 摘要 Prompt 摘要：systemLength=" + length(prompt.getSystemPrompt())
                + "，userLength=" + length(prompt.getUserPrompt()));
    }

    private void printResponseIfEnabled(ChatModelResponse response) {
        if (!properties.isLogResponse()) {
            return;
        }
        System.out.println("视频理解 LLM 摘要响应摘要：model=" + (response == null ? null : response.getModelName())
                + "，totalTokens=" + (response == null ? null : response.getTotalTokens())
                + "，responseLength=" + (response == null ? 0 : response.getResponseLength()));
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
