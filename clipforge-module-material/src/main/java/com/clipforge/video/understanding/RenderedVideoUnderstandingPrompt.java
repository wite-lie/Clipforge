package com.clipforge.video.understanding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RenderedVideoUnderstandingPrompt（已渲染视频理解提示词）。
 *
 * <p>该对象承载视频理解摘要模型调用需要的 system（系统角色）提示词和 user（用户角色）提示词，避免素材模块反向依赖创作模块的 Prompt（提示词）值对象。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenderedVideoUnderstandingPrompt {

    /** systemPrompt（系统提示词）用于定义视频理解摘要模型的角色、边界和输出格式。 */
    private String systemPrompt;

    /** userPrompt（用户提示词）用于承载本次视频元数据、帧摘要、音频文本和风险线索。 */
    private String userPrompt;
}
