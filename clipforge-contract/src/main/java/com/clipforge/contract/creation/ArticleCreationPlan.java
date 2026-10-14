package com.clipforge.contract.creation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ArticleCreationPlan（图文创作方案）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCreationPlan {

    /** theme（创作主题）表示图文内容核心表达方向。 */
    private String theme;

    /** targetAudience（目标受众）表示内容主要面向的人群。 */
    private String targetAudience;

    /** titleCandidates（标题候选）表示可供选择的图文标题。 */
    private List<String> titleCandidates;

    /** summary（摘要）表示文章要点概览。 */
    private String summary;

    /** lead（导语）表示文章开头。 */
    private String lead;

    /** sections（正文段落）表示图文主体结构。 */
    private List<ArticleSection> sections;

    /** imageSlots（配图槽位）表示全文级封面图、段落配图和结尾配图建议。 */
    private List<ArticleImageSlot> imageSlots;

    /** conclusion（结尾）表示收束观点或行动引导。 */
    private String conclusion;

    /** interactionPrompt（互动引导）表示评论、收藏或转发引导。 */
    private String interactionPrompt;

    /** publishCopy（发布文案）表示发布平台正文或摘要区域文案。 */
    private String publishCopy;

    /** hashtags（话题标签）表示平台适配标签。 */
    private List<String> hashtags;

    /** sourceEvidence（来源证据）表示整篇文章使用的关键依据。 */
    private List<ArticleSourceEvidence> sourceEvidence;

    /** riskWarnings（风险提示）表示事实、平台规则或表达边界风险。 */
    private List<String> riskWarnings;

    /** score（效果评分）表示模型对当前图文方案的综合估计。 */
    private int score;

    /** improvementSuggestions（改进建议）表示后续优化方向。 */
    private List<String> improvementSuggestions;
}
