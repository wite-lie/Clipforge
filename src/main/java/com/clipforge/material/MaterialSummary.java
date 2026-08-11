package com.clipforge.material;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MaterialSummary（素材摘要）。
 *
 * <p>该对象把多张图片理解结果合并为一份面向创作规划的素材摘要，后续会注入 Creation Prompt（创作提示词）、Evaluation（评估）和 Revision（修正）链路。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialSummary {

    /** materialCount（素材数量）表示本次摘要覆盖了多少张图片。 */
    private Integer materialCount;

    /** materialIds（素材编号列表）表示参与摘要的素材编号。 */
    private List<String> materialIds;

    /** mainSubjects（主要主体列表）表示多张图片中出现的核心主体。 */
    private List<String> mainSubjects;

    /** scenes（场景列表）表示多张图片覆盖的主要环境和画面场景。 */
    private List<String> scenes;

    /** visualStyles（视觉风格列表）表示图片集合中的主要风格，例如真实摄影、卡通插画等。 */
    private List<String> visualStyles;

    /** keyElements（关键元素列表）表示可以被分镜、旁白和字幕引用的视觉要素。 */
    private List<String> keyElements;

    /** textElements（文字元素列表）表示图片中可见文字，后续 OCR（光学字符识别）能力增强后会更有价值。 */
    private List<String> textElements;

    /** creativeUsages（创作用途列表）表示图片适合放在开头、中段、转场或结尾等位置的建议。 */
    private List<String> creativeUsages;

    /** riskNotes（风险提示列表）表示图片集合中的清晰度、版权、主体不确定或风格不一致风险。 */
    private List<String> riskNotes;

    /** summaryText（摘要文本）表示适合直接注入 Prompt（提示词）的素材总摘要。 */
    private String summaryText;
}
