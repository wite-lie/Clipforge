package com.clipforge.agent.prompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RenderedCreationPlanPrompt（已渲染创作方案提示词）。
 *
 * <p>该对象承载创作方案生成场景需要的 system（系统角色）提示词和 user（用户角色）提示词。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenderedCreationPlanPrompt {

    /** systemPrompt（系统提示词）用于定义模型角色、边界、规则和输出格式。 */
    private String systemPrompt;

    /** userPrompt（用户提示词）用于承载本次创作目标、创作知识和智能体计划。 */
    private String userPrompt;
}
