package com.clipforge.agent.prompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RenderedPrompt（已渲染提示词）。
 *
 * <p>该对象承载一次模型调用需要的 system（系统角色）提示词和 user（用户角色）提示词，可用于创作生成、输出修复、评估等不同场景。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenderedPrompt {

    /** systemPrompt（系统提示词）用于定义模型角色、边界、规则和输出格式。 */
    private String systemPrompt;

    /** userPrompt（用户提示词）用于承载本次动态输入、错误原因或上下文资料。 */
    private String userPrompt;
}
