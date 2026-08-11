package com.clipforge.video.understanding;

import com.clipforge.video.understanding.highlight.VideoHighlightCandidate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoUnderstandingResult（视频理解结果）。
 *
 * <p>该对象是视频理解局部 Agent（智能体）的最终产物，后续会注入创作 Workflow（工作流）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUnderstandingResult {

    /** runId（运行编号）表示本次视频理解所属执行链路。 */
    private String runId;

    /** materialId（素材编号）表示被理解的视频素材编号。 */
    private String materialId;

    /** status（状态）表示视频理解是否完整成功。 */
    private String status;

    /** overallTheme（整体主题）表示视频最适合表达的核心主题。 */
    private String overallTheme;

    /** visualSummary（视觉摘要）表示候选帧呈现出的画面内容。 */
    private String visualSummary;

    /** audioSummary（音频摘要）表示音频转写文本提供的信息。 */
    private String audioSummary;

    /** keyVisualMoments（关键画面线索）表示适合被分镜引用的视觉节点。 */
    private List<String> keyVisualMoments;

    /** highlightCandidates（可能的高光片段）表示兼容旧 Prompt（提示词）的文本片段候选。 */
    private List<String> highlightCandidates;

    /** structuredHighlightCandidates（结构化高光片段）表示带时间、原因和置信度的高光候选。 */
    private List<VideoHighlightCandidate> structuredHighlightCandidates;

    /** creativeAngles（适合创作的卖点）表示可转成标题、钩子或讲解结构的创作角度。 */
    private List<String> creativeAngles;

    /** riskNotes（风险提示）表示素材不足、音频缺失或规则推断不准等问题。 */
    private List<String> riskNotes;

    /** summaryForCreation（创作注入摘要）表示后续创作方案生成 Prompt（提示词）可直接使用的摘要文本。 */
    private String summaryForCreation;

    /** summaryStrategy（摘要策略）表示结果来自规则、LLM（大语言模型）还是降级规则摘要。 */
    private String summaryStrategy;

    /** summaryModelName（摘要模型名称）表示 LLM（大语言模型）摘要器实际使用的模型。 */
    private String summaryModelName;

    /** summaryPromptVersion（摘要提示词版本）用于后续质量复盘和提示词迭代定位。 */
    private String summaryPromptVersion;
}
