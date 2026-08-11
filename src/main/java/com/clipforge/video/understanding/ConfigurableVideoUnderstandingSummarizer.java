package com.clipforge.video.understanding;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * ConfigurableVideoUnderstandingSummarizer（可配置视频理解摘要器）。
 *
 * <p>该组件是 VideoUnderstandingSummarizer（视频理解摘要器）的统一入口，按配置在规则摘要器和 LLM（大语言模型）摘要器之间切换。
 */
@Primary
@Component
@RequiredArgsConstructor
public class ConfigurableVideoUnderstandingSummarizer implements VideoUnderstandingSummarizer {

    /** properties（配置）用于决定摘要模式和 LLM 调用阈值。 */
    private final VideoUnderstandingSummaryProperties properties;

    /** ruleSummarizer（规则摘要器）用于低成本摘要和降级兜底。 */
    private final RuleBasedVideoUnderstandingSummarizer ruleSummarizer;

    /** llmSummarizer（大语言模型摘要器）用于复杂视频主题和高光判断。 */
    private final LLMVideoUnderstandingSummarizer llmSummarizer;

    @Override
    public VideoUnderstandingResult summarize(VideoUnderstandingContext context) {
        VideoUnderstandingSummarizerMode mode = properties.mode();
        if (mode == VideoUnderstandingSummarizerMode.RULE) {
            return ruleSummarizer.summarize(context);
        }
        String insufficientReason = llmInsufficientReason(context);
        if (insufficientReason != null) {
            if (mode == VideoUnderstandingSummarizerMode.LLM_STRICT) {
                throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "视频理解 LLM 摘要前置数据不足：" + insufficientReason);
            }
            return fallback(context, "视频理解 LLM 摘要前置数据不足，已降级规则摘要：" + insufficientReason);
        }
        try {
            return llmSummarizer.summarize(context);
        } catch (RuntimeException exception) {
            if (mode == VideoUnderstandingSummarizerMode.LLM_STRICT) {
                throw exception;
            }
            return fallback(context, "视频理解 LLM 摘要失败，已降级规则摘要：" + exception.getMessage());
        }
    }

    /**
     * llmInsufficientReason（LLM 前置数据不足原因）用于避免在事实过少时浪费模型调用。
     */
    private String llmInsufficientReason(VideoUnderstandingContext context) {
        int frameCount = context == null || context.getFrames() == null ? 0 : context.getFrames().size();
        int minFrames = properties.getMinUsableFrameCount() == null ? 0 : properties.getMinUsableFrameCount();
        if (frameCount < minFrames) {
            return "候选帧数量 " + frameCount + " 小于阈值 " + minFrames;
        }
        String transcript = context == null || context.getAudioContext() == null ? null : context.getAudioContext().getTranscriptText();
        int transcriptLength = transcript == null ? 0 : transcript.strip().length();
        int minAudioLength = properties.getMinAudioTextLength() == null ? 0 : properties.getMinAudioTextLength();
        if (transcriptLength < minAudioLength) {
            return "音频转写文本长度 " + transcriptLength + " 小于阈值 " + minAudioLength;
        }
        return null;
    }

    /**
     * fallback（降级）用于复用规则摘要器，并在结果中留下可复盘原因。
     */
    private VideoUnderstandingResult fallback(VideoUnderstandingContext context, String reason) {
        VideoUnderstandingResult result = ruleSummarizer.summarize(context);
        List<String> riskNotes = result.getRiskNotes() == null ? new ArrayList<>() : new ArrayList<>(result.getRiskNotes());
        riskNotes.add(reason);
        result.setRiskNotes(riskNotes);
        result.setStatus(VideoUnderstandingStatus.PARTIAL_SUCCEEDED.getValue());
        result.setSummaryStrategy("RULE_FALLBACK");
        result.setSummaryModelName(null);
        result.setSummaryPromptVersion(properties.getPromptVersion());
        result.setSummaryForCreation(appendFallbackReason(result.getSummaryForCreation(), reason));
        return result;
    }

    private String appendFallbackReason(String summaryForCreation, String reason) {
        String base = summaryForCreation == null ? "" : summaryForCreation;
        return base + (base.endsWith("\n") ? "" : "\n") + "摘要策略：RULE_FALLBACK；原因：" + reason + "\n";
    }
}
