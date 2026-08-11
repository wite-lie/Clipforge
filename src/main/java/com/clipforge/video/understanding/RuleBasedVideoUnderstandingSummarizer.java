package com.clipforge.video.understanding;

import com.clipforge.material.MaterialSummary;
import com.clipforge.video.understanding.highlight.RuleBasedVideoHighlightRecommender;
import com.clipforge.video.understanding.highlight.VideoHighlightCandidate;
import com.clipforge.video.understanding.highlight.VideoHighlightProperties;
import com.clipforge.video.understanding.highlight.VideoHighlightRecommendRequest;
import com.clipforge.video.understanding.highlight.VideoHighlightRecommendResult;
import com.clipforge.video.understanding.highlight.VideoHighlightRecommender;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RuleBasedVideoUnderstandingSummarizer（规则视频理解摘要器）。
 *
 * <p>该摘要器不调用大模型，而是把元数据、候选帧摘要和音频转写按固定规则组合为第一版视频理解结果。
 */
@Component
public class RuleBasedVideoUnderstandingSummarizer implements VideoUnderstandingSummarizer {

    private static final Pattern MATERIAL_ID_PATTERN = Pattern.compile("(material_frame|video_frame)_[0-9a-f]+");

    /** highlightRecommender（高光推荐器）负责生成结构化高光片段。 */
    private final VideoHighlightRecommender highlightRecommender;

    public RuleBasedVideoUnderstandingSummarizer() {
        this(new RuleBasedVideoHighlightRecommender(new VideoHighlightProperties()));
    }

    @Autowired
    public RuleBasedVideoUnderstandingSummarizer(VideoHighlightRecommender highlightRecommender) {
        this.highlightRecommender = highlightRecommender;
    }

    /**
     * summarize（生成摘要）用于把上下文中的事实线索合成为视频级理解结果。
     *
     * @param context 视频理解上下文
     * @return 视频理解结果
     */
    @Override
    public VideoUnderstandingResult summarize(VideoUnderstandingContext context) {
        // 读取素材摘要。
        MaterialSummary materialSummary = context.getMaterialUnderstandingResult() == null ? null : context.getMaterialUnderstandingResult().getMaterialSummary();
        // 创建风险提示列表。
        List<String> riskNotes = new ArrayList<>();
        // 合并上下文警告。
        if (context.getWarnings() != null) {
            // 添加上下文警告。
            riskNotes.addAll(context.getWarnings());
        }
        // 合并图片素材风险。
        if (materialSummary != null && materialSummary.getRiskNotes() != null) {
            // 添加视觉风险。
            riskNotes.addAll(materialSummary.getRiskNotes());
        }
        // 判断是否有音频文本。
        boolean hasTranscript = context.getAudioContext() != null
                && context.getAudioContext().getTranscriptText() != null
                && !context.getAudioContext().getTranscriptText().isBlank();
        // 如果没有音频文本，则加入风险提示。
        if (!hasTranscript) {
            // 记录音频缺失风险。
            riskNotes.add("当前视频没有可用音频转写文本，视频理解主要依赖画面线索。");
        }
        // 构建视觉摘要。
        String visualSummary = materialSummary == null || materialSummary.getSummaryText() == null
                ? "当前没有可用候选帧视觉摘要。"
                : materialSummary.getSummaryText();
        // 构建音频摘要。
        String audioSummary = hasTranscript
                ? context.getAudioContext().getTranscriptText()
                : "当前没有可用音频摘要。";
        // 构建关键画面线索。
        List<String> keyVisualMoments = buildKeyVisualMoments(context, materialSummary);
        // 构建结构化高光候选片段。
        List<VideoHighlightCandidate> structuredHighlightCandidates = recommendHighlights(context);
        // 构建兼容旧 Prompt（提示词）的文本高光候选。
        List<String> highlightCandidates = formatHighlightCandidates(structuredHighlightCandidates, context, keyVisualMoments);
        // 构建创作角度。
        List<String> creativeAngles = buildCreativeAngles(materialSummary, hasTranscript);
        // 构建整体主题。
        String overallTheme = buildOverallTheme(materialSummary, hasTranscript);
        // 压缩重复且带内部编号的风险提示，避免把大量帧级噪声直接抛给用户。
        riskNotes = compactRiskNotes(riskNotes);
        // 判断整体状态。
        String status = riskNotes.isEmpty() ? VideoUnderstandingStatus.SUCCEEDED.getValue() : VideoUnderstandingStatus.PARTIAL_SUCCEEDED.getValue();
        // 构建可注入创作方案的摘要文本。
        String summaryForCreation = buildSummaryForCreation(context, overallTheme, visualSummary, audioSummary, highlightCandidates, creativeAngles, riskNotes);
        // 返回结构化视频理解结果。
        return VideoUnderstandingResult.builder()
                .runId(context.getRunId())
                .materialId(context.getMaterialId())
                .status(status)
                .overallTheme(overallTheme)
                .visualSummary(visualSummary)
                .audioSummary(audioSummary)
                .keyVisualMoments(keyVisualMoments)
                .highlightCandidates(highlightCandidates)
                .structuredHighlightCandidates(structuredHighlightCandidates)
                .creativeAngles(creativeAngles)
                .riskNotes(riskNotes)
                .summaryForCreation(summaryForCreation)
                .summaryStrategy("RULE")
                .summaryModelName(null)
                .summaryPromptVersion("rule-based-v1")
                .build();
    }

    /**
     * buildKeyVisualMoments（构建关键画面线索）用于把候选帧时间点和图片摘要合并成人能读懂的画面节点。
     *
     * @param context 视频理解上下文
     * @param materialSummary 素材摘要
     * @return 关键画面线索列表
     */
    private List<String> buildKeyVisualMoments(VideoUnderstandingContext context, MaterialSummary materialSummary) {
        // 如果没有候选帧，则返回空列表。
        if (context.getFrames() == null || context.getFrames().isEmpty()) {
            // 返回空画面线索。
            return List.of();
        }
        // 读取关键元素列表。
        List<String> keyElements = materialSummary == null || materialSummary.getKeyElements() == null ? List.of() : materialSummary.getKeyElements();
        // 创建结果列表。
        List<String> moments = new ArrayList<>();
        // 遍历候选帧。
        for (VideoUnderstandingFrameContext frame : context.getFrames()) {
            // 读取当前帧可使用的关键元素。
            String element = keyElements.isEmpty() ? "候选帧画面" : keyElements.get(Math.min(moments.size(), keyElements.size() - 1));
            // 组装关键画面线索。
            moments.add(formatSeconds(frame.getTimestampSeconds()) + " 秒附近：" + element);
        }
        // 返回关键画面线索。
        return moments;
    }

    /**
     * buildHighlightCandidates（构建高光候选）用于基于候选帧时间点给出粗粒度片段建议。
     *
     * @param context 视频理解上下文
     * @param keyVisualMoments 关键画面线索
     * @return 高光候选列表
     */
    private List<String> buildLegacyHighlightCandidates(VideoUnderstandingContext context, List<String> keyVisualMoments) {
        // 如果没有关键画面，则返回空列表。
        if (keyVisualMoments == null || keyVisualMoments.isEmpty()) {
            // 返回空高光候选。
            return List.of();
        }
        // 创建结果列表。
        List<String> candidates = new ArrayList<>();
        // 遍历前 3 个关键画面，避免第一版输出过长。
        for (int index = 0; index < Math.min(3, keyVisualMoments.size()); index++) {
            // 读取帧上下文。
            VideoUnderstandingFrameContext frame = context.getFrames().get(index);
            // 计算片段开始时间。
            double start = Math.max(0D, nullToZero(frame.getTimestampSeconds()) - 1.5D);
            // 计算片段结束时间。
            double end = nullToZero(frame.getTimestampSeconds()) + 2.5D;
            // 添加候选片段说明。
            candidates.add(formatSeconds(start) + "-" + formatSeconds(end) + " 秒：围绕“" + keyVisualMoments.get(index) + "”组织讲解或转场。");
        }
        // 返回高光候选。
        return candidates;
    }

    /**
     * recommendHighlights（推荐高光）用于调用结构化高光推荐器。
     */
    private List<VideoHighlightCandidate> recommendHighlights(VideoUnderstandingContext context) {
        VideoHighlightRecommendResult result = highlightRecommender.recommend(VideoHighlightRecommendRequest.builder()
                .context(context)
                .build());
        return result == null || result.getCandidates() == null ? List.of() : result.getCandidates();
    }

    /**
     * formatHighlightCandidates（格式化高光候选）用于兼容旧的字符串高光字段。
     */
    private List<String> formatHighlightCandidates(List<VideoHighlightCandidate> candidates, VideoUnderstandingContext context, List<String> keyVisualMoments) {
        if (candidates == null || candidates.isEmpty()) {
            return buildLegacyHighlightCandidates(context, keyVisualMoments);
        }
        return candidates.stream()
                .map(candidate -> formatSeconds(candidate.getStartSeconds()) + "-" + formatSeconds(candidate.getEndSeconds())
                        + " 秒：" + candidate.getReason() + "置信度：" + String.format("%.2f", candidate.getConfidence() == null ? 0D : candidate.getConfidence()))
                .toList();
    }

    /**
     * buildCreativeAngles（构建创作角度）用于从视觉主体和音频文本中提炼视频卖点。
     *
     * @param materialSummary 素材摘要
     * @param hasTranscript 是否存在音频文本
     * @return 创作角度列表
     */
    private List<String> buildCreativeAngles(MaterialSummary materialSummary, boolean hasTranscript) {
        // 创建创作角度列表。
        List<String> angles = new ArrayList<>();
        // 如果素材摘要提供主体，则围绕主体生成角度。
        if (materialSummary != null && materialSummary.getMainSubjects() != null && !materialSummary.getMainSubjects().isEmpty()) {
            // 添加主体科普角度。
            angles.add("围绕“" + materialSummary.getMainSubjects().get(0) + "”做短视频科普开场。");
        }
        // 如果素材摘要提供创作用途，则复用为角度。
        if (materialSummary != null && materialSummary.getCreativeUsages() != null) {
            // 添加创作用途。
            angles.addAll(materialSummary.getCreativeUsages().stream().limit(3).toList());
        }
        // 如果存在转写文本，则增加视听结合角度。
        if (hasTranscript) {
            // 添加音画结合角度。
            angles.add("结合音频转写文本和候选帧画面，做“画面证据 + 旁白解释”的科普结构。");
        }
        // 如果没有角度，则给出兜底角度。
        if (angles.isEmpty()) {
            // 添加兜底角度。
            angles.add("以候选帧画面变化作为叙事线索，生成基础视频摘要和创作规划。");
        }
        // 返回创作角度。
        return angles;
    }

    /**
     * buildOverallTheme（构建整体主题）用于生成一句视频级主题。
     *
     * @param materialSummary 素材摘要
     * @param hasTranscript 是否存在音频文本
     * @return 整体主题
     */
    private String buildOverallTheme(MaterialSummary materialSummary, boolean hasTranscript) {
        // 如果有主体，则优先使用主体构建主题。
        if (materialSummary != null && materialSummary.getMainSubjects() != null && !materialSummary.getMainSubjects().isEmpty()) {
            // 返回主体主题。
            return "围绕“" + materialSummary.getMainSubjects().get(0) + "”的视频内容理解与创作规划线索";
        }
        // 如果只有音频，则返回音频主题。
        if (hasTranscript) {
            // 返回音频主题。
            return "基于音频转写文本的视频内容理解";
        }
        // 返回兜底主题。
        return "基于有限视频素材线索的内容理解";
    }

    /**
     * buildSummaryForCreation（构建创作注入摘要）用于生成后续创作 Workflow（工作流）可直接拼入 Prompt（提示词）的文本。
     *
     * @param context 视频理解上下文
     * @param overallTheme 整体主题
     * @param visualSummary 视觉摘要
     * @param audioSummary 音频摘要
     * @param highlightCandidates 高光候选
     * @param creativeAngles 创作角度
     * @param riskNotes 风险提示
     * @return 创作注入摘要
     */
    private String buildSummaryForCreation(VideoUnderstandingContext context, String overallTheme, String visualSummary, String audioSummary, List<String> highlightCandidates, List<String> creativeAngles, List<String> riskNotes) {
        // 使用 StringBuilder（字符串构建器）拼接摘要。
        StringBuilder builder = new StringBuilder();
        // 写入标题。
        builder.append("【视频理解摘要】\n");
        // 写入素材编号。
        builder.append("视频素材编号：").append(context.getMaterialId()).append("\n");
        // 写入整体主题。
        builder.append("整体主题：").append(overallTheme).append("\n");
        // 写入视频基本信息。
        if (context.getMetadata() != null) {
            // 拼接时长和尺寸。
            builder.append("视频信息：时长约 ").append(formatSeconds(context.getMetadata().getDurationSeconds())).append(" 秒，画面尺寸 ")
                    .append(context.getMetadata().getWidth()).append("x").append(context.getMetadata().getHeight()).append("。\n");
        }
        // 写入视觉摘要。
        builder.append("视觉摘要：").append(visualSummary).append("\n");
        // 写入音频摘要。
        builder.append("音频摘要：").append(audioSummary).append("\n");
        // 写入高光候选。
        builder.append("可能高光片段：").append(highlightCandidates).append("\n");
        // 写入创作角度。
        builder.append("适合创作的卖点：").append(creativeAngles).append("\n");
        // 写入风险提示。
        builder.append("风险提示：").append(riskNotes).append("\n");
        // 返回摘要文本。
        return builder.toString();
    }

    /**
     * compactRiskNotes（压缩风险提示）用于合并重复的帧级内部噪声。
     *
     * @param riskNotes 原始风险提示
     * @return 压缩后的风险提示
     */
    private List<String> compactRiskNotes(List<String> riskNotes) {
        if (riskNotes == null || riskNotes.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String note : riskNotes) {
            if (note == null || note.isBlank()) {
                continue;
            }
            String normalized = normalizeRiskNote(note);
            counts.put(normalized, counts.getOrDefault(normalized, 0) + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> entry.getValue() > 1 ? entry.getKey() + "（共" + entry.getValue() + "条）" : entry.getKey())
                .toList();
    }

    /**
     * normalizeRiskNote（规范化风险提示）用于去掉内部素材编号。
     */
    private String normalizeRiskNote(String note) {
        String normalized = MATERIAL_ID_PATTERN.matcher(note).replaceAll("[内部帧素材]");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    /**
     * formatSeconds（格式化秒数）用于统一输出一位小数时间。
     *
     * @param seconds 秒数
     * @return 格式化文本
     */
    private String formatSeconds(Double seconds) {
        // 如果秒数为空，则按 0 秒处理。
        return String.format("%.1f", nullToZero(seconds));
    }

    /**
     * nullToZero（空值转零）用于避免时间计算空指针。
     *
     * @param value 原始数值
     * @return 非空数值
     */
    private double nullToZero(Double value) {
        // 空值返回 0。
        return value == null ? 0D : value;
    }
}
