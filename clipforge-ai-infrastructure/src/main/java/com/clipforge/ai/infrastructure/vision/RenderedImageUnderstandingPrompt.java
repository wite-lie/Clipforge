package com.clipforge.ai.infrastructure.vision;
import com.clipforge.ai.api.vision.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RenderedImageUnderstandingPrompt（已渲染图片理解提示词）。
 *
 * <p>该对象保存一次图片理解调用最终发送给 VLM（视觉语言模型）的 system/user（系统/用户）提示词。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenderedImageUnderstandingPrompt {

    /** systemPrompt（系统提示词）定义视觉模型角色、边界和输出格式。 */
    private String systemPrompt;

    /** userPrompt（用户提示词）承载理解指令和图片素材清单。 */
    private String userPrompt;
}
