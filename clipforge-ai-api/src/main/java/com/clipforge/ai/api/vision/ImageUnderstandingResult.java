package com.clipforge.ai.api.vision;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ImageUnderstandingResult（图片理解结果）。
 *
 * <p>该对象用结构化字段描述一张图片中对创作规划有价值的信息，后续会进入创作方案 Prompt（提示词）和素材相关性评估。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUnderstandingResult {

    /** materialId（素材编号）表示该理解结果对应哪张图片。 */
    private String materialId;

    /** mainSubject（主体）表示图片最重要的主体对象。 */
    private String mainSubject;

    /** scene（场景）表示图片发生或呈现的环境。 */
    private String scene;

    /** visualStyle（视觉风格）表示图片是真实摄影、卡通插画、信息图等风格。 */
    private String visualStyle;

    /** keyElements（关键元素）表示画面中适合被脚本引用的视觉要素。 */
    private List<String> keyElements;

    /** textElements（文字元素）表示图片中可见文字，Phase 6.3（阶段 6.3）先预留给后续 OCR（光学字符识别）。 */
    private List<String> textElements;

    /** creativeUsage（创作用途）表示该图片适合放在开头、中段解释、结尾总结等位置。 */
    private String creativeUsage;

    /** riskNotes（风险提示）表示图片可能存在的识别不确定、版权、低清晰度或风格不一致问题。 */
    private List<String> riskNotes;

    /** summary（摘要）表示面向创作规划的单图简短总结。 */
    private String summary;
}
