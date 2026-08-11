package com.clipforge.agent;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationPlanResult（创作方案结果）。
 *
 * <p>该对象承载 V1（第一版）最小创作规划链路生成的结构化内容。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreationPlanResult {

    /** theme（创作主题）表示本次内容的核心表达方向。 */
    private String theme;

    /** targetAudience（目标受众）表示内容主要面向的人群。 */
    private String targetAudience;

    /** hook（开头钩子）表示视频或图文开头吸引用户注意的句子。 */
    private String hook;

    /** storyboard（分镜脚本）表示内容展开的镜头或段落安排。 */
    private List<String> storyboard;

    /** voiceoverScript（旁白脚本）表示视频口播或旁白内容。 */
    private String voiceoverScript;

    /** subtitleSuggestions（字幕建议）表示适合叠加在画面上的字幕。 */
    private List<String> subtitleSuggestions;

    /** titleCandidates（标题候选）表示可供选择的标题列表。 */
    private List<String> titleCandidates;

    /** publishCopy（发布文案）表示发布时正文区域使用的文案。 */
    private String publishCopy;

    /** hashtags（话题标签）表示适配平台的话题标签列表。 */
    private List<String> hashtags;

    /** score（效果评分）表示当前方案的模拟综合评分。 */
    private int score;

    /** improvementSuggestions（改进建议）表示后续优化方向。 */
    private List<String> improvementSuggestions;
}
