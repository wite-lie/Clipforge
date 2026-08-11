package com.clipforge.video.understanding.highlight;

import com.clipforge.material.MaterialSummary;
import com.clipforge.video.understanding.VideoUnderstandingContext;
import com.clipforge.video.understanding.VideoUnderstandingFrameContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * RuleBasedVideoHighlightRecommender（规则视频高光推荐器）。
 *
 * <p>该实现不调用模型，基于候选帧时间点、关键帧分数、视觉主体、字幕重叠和用户目标关键词生成高光片段。
 */
@Component
public class RuleBasedVideoHighlightRecommender implements VideoHighlightRecommender {

    /** properties（配置）用于控制窗口、数量和最低置信度。 */
    private final VideoHighlightProperties properties;

    public RuleBasedVideoHighlightRecommender(VideoHighlightProperties properties) {
        this.properties = properties == null ? new VideoHighlightProperties() : properties;
    }

    @Override
    public VideoHighlightRecommendResult recommend(VideoHighlightRecommendRequest request) {
        if (!properties.isEnabled() || request == null || request.getContext() == null) {
            return VideoHighlightRecommendResult.builder().candidates(List.of()).build();
        }
        VideoUnderstandingContext context = request.getContext();
        if (context.getFrames() == null || context.getFrames().isEmpty()) {
            return VideoHighlightRecommendResult.builder().candidates(List.of()).build();
        }
        MaterialSummary materialSummary = context.getMaterialUnderstandingResult() == null
                ? null
                : context.getMaterialUnderstandingResult().getMaterialSummary();
        List<String> targetTerms = collectTargetTerms(context, materialSummary);
        List<VideoHighlightCandidate> candidates = new ArrayList<>();
        for (VideoUnderstandingFrameContext frame : context.getFrames()) {
            VideoHighlightCandidate candidate = buildCandidate(context, materialSummary, targetTerms, frame);
            if (candidate.getConfidence() >= minConfidence()) {
                candidates.add(candidate);
            }
        }
        List<VideoHighlightCandidate> ranked = candidates.stream()
                .sorted(Comparator.comparing(VideoHighlightCandidate::getConfidence, Comparator.reverseOrder()))
                .limit(topK())
                .toList();
        return VideoHighlightRecommendResult.builder().candidates(ranked).build();
    }

    /**
     * buildCandidate（构建候选）用于围绕单个候选帧生成高光片段和评分。
     */
    private VideoHighlightCandidate buildCandidate(VideoUnderstandingContext context, MaterialSummary materialSummary,
                                                   List<String> targetTerms, VideoUnderstandingFrameContext frame) {
        double timestamp = nullToZero(frame.getTimestampSeconds());
        double start = Math.max(0D, timestamp - preWindowSeconds());
        double end = timestamp + postWindowSeconds();
        Set<String> evidenceTypes = new LinkedHashSet<>();
        Set<String> keywords = new LinkedHashSet<>();
        double score = 0.15D;

        if (Integer.valueOf(1).equals(frame.getKeyFrameSelected())) {
            score += 0.25D;
            evidenceTypes.add(VideoHighlightEvidenceType.KEY_FRAME.name());
        }
        if (frame.getKeyFrameScore() != null && frame.getKeyFrameScore() > 0) {
            score += Math.min(0.15D, frame.getKeyFrameScore() * 0.15D);
            evidenceTypes.add(VideoHighlightEvidenceType.KEY_FRAME.name());
        }
        if (frame.getQualityScore() != null && frame.getQualityScore() >= 0.6D) {
            score += 0.1D;
            evidenceTypes.add(VideoHighlightEvidenceType.FRAME_QUALITY.name());
        }
        if (matchesMaterialSummary(materialSummary, targetTerms, keywords)) {
            score += 0.2D;
            evidenceTypes.add(VideoHighlightEvidenceType.VISUAL_SUBJECT.name());
        }
        List<VideoSubtitleSegmentContext> nearbySubtitles = nearbySubtitles(context.getSubtitleSegments(), timestamp);
        if (!nearbySubtitles.isEmpty()) {
            score += 0.15D;
            evidenceTypes.add(VideoHighlightEvidenceType.SUBTITLE_OVERLAP.name());
            List<String> subtitleKeywords = matchedSubtitleKeywords(nearbySubtitles, targetTerms);
            if (!subtitleKeywords.isEmpty()) {
                score += 0.2D;
                evidenceTypes.add(VideoHighlightEvidenceType.SUBTITLE_KEYWORD.name());
                keywords.addAll(subtitleKeywords);
            }
        }
        if (timestamp <= openingHookSeconds() && containsAny(context.getInstruction(), "开头", "钩子", "悬念", "前3秒", "前三秒")) {
            score += 0.1D;
            evidenceTypes.add(VideoHighlightEvidenceType.OPENING_HOOK.name());
            keywords.add("开头钩子");
        }
        if (frame.getRejectReason() != null && !frame.getRejectReason().isBlank()) {
            score -= 0.15D;
        }
        double confidence = round(Math.max(0D, Math.min(1D, score)));
        return VideoHighlightCandidate.builder()
                .startSeconds(round(start))
                .endSeconds(round(end))
                .reason(buildReason(frame, nearbySubtitles, keywords, evidenceTypes))
                .confidence(confidence)
                .evidenceTypes(new ArrayList<>(evidenceTypes))
                .keywords(new ArrayList<>(keywords))
                .build();
    }

    /**
     * collectTargetTerms（收集目标词）用于合并用户指令、视觉主体和高价值通用词。
     */
    private List<String> collectTargetTerms(VideoUnderstandingContext context, MaterialSummary materialSummary) {
        Set<String> terms = new LinkedHashSet<>();
        addTerms(terms, splitText(context.getInstruction()));
        if (materialSummary != null) {
            addTerms(terms, materialSummary.getMainSubjects());
            addTerms(terms, materialSummary.getKeyElements());
            addTerms(terms, materialSummary.getScenes());
        }
        addTerms(terms, List.of("虎鲸", "顶级猎手", "保护", "捕食", "协作", "社会化", "海洋", "高智商", "背鳍"));
        return terms.stream().filter(this::hasText).toList();
    }

    private boolean matchesMaterialSummary(MaterialSummary summary, List<String> targetTerms, Set<String> keywords) {
        if (summary == null || targetTerms == null || targetTerms.isEmpty()) {
            return false;
        }
        String joined = String.join(" ", safeList(summary.getMainSubjects())) + " "
                + String.join(" ", safeList(summary.getKeyElements())) + " "
                + String.join(" ", safeList(summary.getScenes())) + " "
                + String.join(" ", safeList(summary.getCreativeUsages())) + " "
                + (summary.getSummaryText() == null ? "" : summary.getSummaryText());
        boolean matched = false;
        for (String term : targetTerms) {
            if (containsIgnoreCase(joined, term)) {
                keywords.add(term);
                matched = true;
            }
        }
        return matched;
    }

    private List<VideoSubtitleSegmentContext> nearbySubtitles(List<VideoSubtitleSegmentContext> subtitles, double timestamp) {
        if (subtitles == null || subtitles.isEmpty()) {
            return List.of();
        }
        double window = subtitleOverlapWindowSeconds();
        return subtitles.stream()
                .filter(segment -> segment != null && overlaps(segment, timestamp - window, timestamp + window))
                .toList();
    }

    private boolean overlaps(VideoSubtitleSegmentContext segment, double start, double end) {
        double segmentStart = nullToZero(segment.getStartSeconds());
        double segmentEnd = segment.getEndSeconds() == null ? segmentStart : segment.getEndSeconds();
        return segmentStart <= end && segmentEnd >= start;
    }

    private List<String> matchedSubtitleKeywords(List<VideoSubtitleSegmentContext> subtitles, List<String> targetTerms) {
        Set<String> result = new LinkedHashSet<>();
        for (VideoSubtitleSegmentContext subtitle : subtitles) {
            String text = subtitle.getText();
            for (String term : targetTerms) {
                if (containsIgnoreCase(text, term)) {
                    result.add(term);
                }
            }
        }
        return result.stream().toList();
    }

    private String buildReason(VideoUnderstandingFrameContext frame, List<VideoSubtitleSegmentContext> nearbySubtitles,
                               Set<String> keywords, Set<String> evidenceTypes) {
        StringBuilder builder = new StringBuilder();
        builder.append(formatSeconds(frame.getTimestampSeconds())).append(" 秒附近存在候选关键画面");
        if (!keywords.isEmpty()) {
            builder.append("，命中关键词：").append(String.join("、", keywords.stream().limit(5).toList()));
        }
        if (nearbySubtitles != null && !nearbySubtitles.isEmpty()) {
            builder.append("，附近字幕包含可讲解信息");
        }
        if (evidenceTypes.contains(VideoHighlightEvidenceType.OPENING_HOOK.name())) {
            builder.append("，适合作为开头钩子");
        }
        builder.append("。");
        return builder.toString();
    }

    private List<String> splitText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.replaceAll("[，。！？、；：,.!?;:\\n\\r\\t]", " ").split("\\s+"))
                .filter(this::hasText)
                .filter(value -> value.length() >= 2)
                .toList();
    }

    private void addTerms(Set<String> target, List<String> values) {
        if (target == null || values == null) {
            return;
        }
        values.stream().filter(this::hasText).map(String::strip).forEach(target::add);
    }

    private boolean containsAny(String text, String... terms) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String term : terms) {
            if (containsIgnoreCase(text, term)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String text, String term) {
        return text != null && term != null && text.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String formatSeconds(Double seconds) {
        return String.format("%.1f", nullToZero(seconds));
    }

    private double nullToZero(Double value) {
        return value == null ? 0D : value;
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private int topK() {
        return properties.getTopK() == null || properties.getTopK() <= 0 ? 5 : properties.getTopK();
    }

    private double preWindowSeconds() {
        return properties.getPreWindowSeconds() == null ? 1.5D : properties.getPreWindowSeconds();
    }

    private double postWindowSeconds() {
        return properties.getPostWindowSeconds() == null ? 3.0D : properties.getPostWindowSeconds();
    }

    private double subtitleOverlapWindowSeconds() {
        return properties.getSubtitleOverlapWindowSeconds() == null ? 2.0D : properties.getSubtitleOverlapWindowSeconds();
    }

    private double minConfidence() {
        return properties.getMinConfidence() == null ? 0.35D : properties.getMinConfidence();
    }

    private double openingHookSeconds() {
        return properties.getOpeningHookSeconds() == null ? 5.0D : properties.getOpeningHookSeconds();
    }
}
