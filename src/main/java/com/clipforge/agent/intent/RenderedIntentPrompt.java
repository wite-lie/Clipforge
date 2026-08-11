package com.clipforge.agent.intent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RenderedIntentPrompt（已渲染意图提示词）。
 *
 * <p>该对象承载发送给 LLM（大语言模型）意图分类器的 system/user（系统/用户）提示词。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenderedIntentPrompt {

    /** systemPrompt（系统提示词）表示意图分类的角色、边界和输出格式。 */
    private String systemPrompt;

    /** userPrompt（用户提示词）表示本轮用户输入和结构化上下文。 */
    private String userPrompt;
}
