package com.clipforge.video.understanding;

import com.clipforge.agent.prompt.RenderedPrompt;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.VideoMetadataEntity;
import com.clipforge.material.MaterialSummary;
import com.clipforge.video.understanding.highlight.VideoSubtitleSegmentContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * VideoUnderstandingPromptRenderer（视频理解提示词渲染器）。
 *
 * <p>该组件从 Prompt（提示词）文件读取模板，并把视频元数据、候选帧、视觉摘要和音频转写渲染为 LLM（大语言模型）输入。
 */
@Component
@RequiredArgsConstructor
public class VideoUnderstandingPromptRenderer {

    /** properties（配置）用于读取提示词路径和裁剪长度。 */
    private final VideoUnderstandingSummaryProperties properties;

    /** resourceLoader（资源加载器）用于读取 classpath（类路径）提示词文件。 */
    private final ResourceLoader resourceLoader;

    /**
     * render（渲染）用于生成视频理解摘要模型的 system/user（系统/用户）提示词。
     */
    public RenderedPrompt render(VideoUnderstandingContext context) {
        String systemPrompt = readResource(properties.getSystemPath());
        String userPrompt = readResource(properties.getUserPath())
                .replace("{{instruction}}", safe(context == null ? null : context.getInstruction(), 1200))
                .replace("{{metadata}}", renderMetadata(context == null ? null : context.getMetadata()))
                .replace("{{frames}}", renderFrames(context == null ? null : context.getFrames()))
                .replace("{{frameSummary}}", renderFrameSummary(context))
                .replace("{{audioTranscript}}", renderAudioTranscript(context))
                .replace("{{subtitleSegments}}", renderSubtitleSegments(context == null ? null : context.getSubtitleSegments()))
                .replace("{{warnings}}", renderWarnings(context == null ? null : context.getWarnings()));
        return RenderedPrompt.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .build();
    }

    /**
     * renderMetadata（渲染元数据）用于让模型知道时长、分辨率和基础媒体信息。
     */
    private String renderMetadata(VideoMetadataEntity metadata) {
        if (metadata == null) {
            return "当前没有可用视频元数据。";
        }
        return "时长：" + value(metadata.getDurationSeconds()) + " 秒\n"
                + "画面尺寸：" + value(metadata.getWidth()) + "x" + value(metadata.getHeight()) + "\n"
                + "视频编码：" + value(metadata.getVideoCodec()) + "\n"
                + "音频编码：" + value(metadata.getAudioCodec()) + "\n"
                + "是否有音轨：" + value(metadata.getHasAudio());
    }

    /**
     * renderFrames（渲染候选帧）用于把关键帧时间点和素材编号传给模型。
     */
    private String renderFrames(List<VideoUnderstandingFrameContext> frames) {
        if (frames == null || frames.isEmpty()) {
            return "当前没有可用候选帧。";
        }
        StringBuilder builder = new StringBuilder();
        for (VideoUnderstandingFrameContext frame : frames) {
            builder.append("- frameId：").append(value(frame.getFrameId()))
                    .append("；frameMaterialId：").append(value(frame.getFrameMaterialId()))
                    .append("；frameIndex：").append(value(frame.getFrameIndex()))
                    .append("；timestampSeconds：").append(value(frame.getTimestampSeconds()))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    /**
     * renderFrameSummary（渲染帧摘要）用于注入候选帧图片理解合并摘要。
     */
    private String renderFrameSummary(VideoUnderstandingContext context) {
        MaterialSummary summary = context == null || context.getMaterialUnderstandingResult() == null
                ? null
                : context.getMaterialUnderstandingResult().getMaterialSummary();
        if (summary == null) {
            return "当前没有可用候选帧视觉摘要。";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("摘要文本：").append(safe(summary.getSummaryText(), properties.getMaxFrameSummaryChars())).append(System.lineSeparator());
        builder.append("主要主体：").append(formatList(summary.getMainSubjects())).append(System.lineSeparator());
        builder.append("场景：").append(formatList(summary.getScenes())).append(System.lineSeparator());
        builder.append("视觉风格：").append(formatList(summary.getVisualStyles())).append(System.lineSeparator());
        builder.append("关键元素：").append(formatList(summary.getKeyElements())).append(System.lineSeparator());
        builder.append("创作用途：").append(formatList(summary.getCreativeUsages())).append(System.lineSeparator());
        builder.append("视觉风险：").append(formatList(summary.getRiskNotes()));
        return compact(builder.toString(), properties.getMaxFrameSummaryChars());
    }

    /**
     * renderAudioTranscript（渲染音频转写）用于注入 ASR（自动语音识别）文本。
     */
    private String renderAudioTranscript(VideoUnderstandingContext context) {
        if (context == null || context.getAudioContext() == null) {
            return "当前没有可用音频上下文。";
        }
        VideoUnderstandingAudioContext audio = context.getAudioContext();
        String transcript = safe(audio.getTranscriptText(), properties.getMaxAudioTranscriptChars());
        if (transcript.isBlank()) {
            return "当前没有可用音频转写文本。speechRecognitionStatus=" + value(audio.getSpeechRecognitionStatus())
                    + "；errorMessage=" + value(audio.getErrorMessage());
        }
        return compact(transcript, properties.getMaxAudioTranscriptChars());
    }

    /**
     * renderSubtitleSegments（渲染字幕片段）用于给模型提供带时间戳的音频线索。
     */
    private String renderSubtitleSegments(List<VideoSubtitleSegmentContext> subtitleSegments) {
        if (subtitleSegments == null || subtitleSegments.isEmpty()) {
            return "当前没有可用字幕时间线。";
        }
        StringBuilder builder = new StringBuilder();
        for (VideoSubtitleSegmentContext segment : subtitleSegments) {
            builder.append("- ")
                    .append(value(segment.getStartSeconds())).append("-").append(value(segment.getEndSeconds()))
                    .append(" 秒：").append(safe(segment.getText(), 300))
                    .append(System.lineSeparator());
        }
        return compact(builder.toString(), properties.getMaxAudioTranscriptChars());
    }

    /**
     * renderWarnings（渲染警告）用于告诉模型哪些事实存在缺口。
     */
    private String renderWarnings(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return "无";
        }
        return formatList(warnings);
    }

    /**
     * readResource（读取资源）用于按 UTF-8（统一码转换格式）读取 Prompt（提示词）。
     */
    private String readResource(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_ERROR, "视频理解摘要 Prompt 文件不存在：" + path);
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_ERROR, "视频理解摘要 Prompt 文件读取失败：" + path, exception);
        }
    }

    private String formatList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "无";
        }
        return String.join("；", values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .toList());
    }

    private String safe(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return compact(value.strip(), maxLength);
    }

    private String compact(String value, Integer maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int limit = maxLength == null || maxLength <= 0 ? 1000 : maxLength;
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit) + "...<已截断>";
    }

    private String value(Object value) {
        return value == null ? "未知" : String.valueOf(value);
    }
}
