package com.clipforge.model.routing;

/**
 * ModelCapability（模型能力）。
 *
 * <p>该枚举用于区分不同模型调用能力，方便模型路由、健康检查、故障转移和成本复盘按能力维度聚合。
 */
public enum ModelCapability {

    /** CHAT（聊天）表示大语言模型能力。 */
    CHAT("chat"),

    /** VISION（视觉）表示视觉语言模型能力。 */
    VISION("vision"),

    /** EMBEDDING（嵌入）表示文本向量化能力。 */
    EMBEDDING("embedding"),

    /** RERANK（重排序）表示 Cross-Encoder（交叉编码器）重排能力。 */
    RERANK("rerank"),

    /** SPEECH（语音）表示 ASR（自动语音识别）能力。 */
    SPEECH("speech");

    private final String configKey;

    ModelCapability(String configKey) {
        this.configKey = configKey;
    }

    /**
     * configKey（配置键）用于在配置文件里区分不同能力。
     *
     * @return 配置键
     */
    public String configKey() {
        return configKey;
    }
}
