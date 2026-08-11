package com.clipforge.video.understanding;

/**
 * VideoUnderstandingSummarizerMode（视频理解摘要模式）。
 *
 * <p>该枚举用于控制视频理解摘要阶段是否调用 LLM（大语言模型），以及失败时是否允许规则降级。
 */
public enum VideoUnderstandingSummarizerMode {

    /** RULE（规则）表示只使用规则摘要器，不额外消耗模型 Token（词元）。 */
    RULE,

    /** LLM（大语言模型）表示优先调用 LLM，前置条件不足或调用失败时降级规则摘要。 */
    LLM,

    /** LLM_STRICT（严格大语言模型）表示必须调用 LLM，前置条件不足或调用失败时直接失败。 */
    LLM_STRICT;

    /**
     * fromConfig（从配置解析）用于兼容 rule、llm 和 llm-strict 等配置写法。
     */
    public static VideoUnderstandingSummarizerMode fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return RULE;
        }
        return switch (value.strip().toLowerCase()) {
            case "llm" -> LLM;
            case "llm-strict", "llm_strict" -> LLM_STRICT;
            default -> RULE;
        };
    }
}
