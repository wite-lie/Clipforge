package com.clipforge.material.video.understanding;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VideoUnderstandingCreationContext（视频理解创作上下文）。
 *
 * <p>该对象面向创作 Workflow（工作流），只承载已经完成处理的视频理解摘要，不触发抽帧、音频提取或模型调用。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUnderstandingCreationContext {

    /** videoMaterialIds（视频素材编号列表）表示本次创作明确选择的视频素材。 */
    private List<String> videoMaterialIds;

    /** summaries（视频理解结果列表）表示可用于创作的结构化视频理解结果。 */
    private List<VideoUnderstandingResult> summaries;

    /** summaryText（摘要文本）表示可直接注入 Prompt（提示词）的视频理解上下文。 */
    private String summaryText;

    /** warnings（警告列表）表示视频理解结果缺失、不可用或解析失败等非致命问题。 */
    private List<String> warnings;

    /**
     * hasAvailableSummary（是否存在可用摘要）用于判断是否应注入 Prompt（提示词）。
     *
     * @return 是否存在可用视频摘要
     */
    public boolean hasAvailableSummary() {
        // 如果摘要文本为空，则认为没有可用摘要。
        return summaryText != null && !summaryText.isBlank();
    }
}
