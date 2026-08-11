package com.clipforge.model.governance;

/**
 * ModelCallType（模型调用类型）用于区分不同模型能力的并发池。
 *
 * <p>不同类型分开限流，可以避免图片理解、知识库向量化、Rerank（重排序）和创作生成互相抢占全部模型额度。
 */
public enum ModelCallType {

    /** 创作方案生成对应主 LLM（大语言模型）调用。 */
    CHAT_GENERATION("chat-generation"),

    /** 输出修复对应 JSON（对象表示格式）修复类 LLM（大语言模型）调用。 */
    CHAT_REPAIR("chat-repair"),

    /** LLM-as-Judge（大模型裁判）评估调用。 */
    CHAT_EVALUATION("chat-evaluation"),

    /** Reflection（反思修正）调用。 */
    CHAT_REVISION("chat-revision"),

    /** VLM（视觉语言模型）图片理解调用。 */
    VISION_UNDERSTANDING("vision-understanding"),

    /** Embedding（嵌入）向量化调用。 */
    EMBEDDING("embedding"),

    /** Rerank（重排序）模型调用。 */
    RERANK("rerank"),

    /** ASR（自动语音识别）模型调用，当前先预留。 */
    SPEECH_ASR("speech-asr");

    /** configKey（配置键）用于匹配 YAML（配置文件）里的 limits 子项。 */
    private final String configKey;

    /**
     * 构造函数用于保存配置键。
     *
     * @param configKey 配置键
     */
    ModelCallType(String configKey) {
        this.configKey = configKey;
    }

    /**
     * configKey（配置键）用于读取单个调用类型的限流配置。
     *
     * @return 配置键
     */
    public String configKey() {
        return configKey;
    }

    /**
     * fromChatPurpose（从聊天用途推断调用类型）把现有 ChatModelRequest.purpose（调用用途）映射到限流池。
     *
     * @param purpose 调用用途
     * @return 模型调用类型
     */
    public static ModelCallType fromChatPurpose(String purpose) {
        // 没有明确用途时，默认归入创作生成池。
        if (purpose == null || purpose.isBlank()) {
            return CHAT_GENERATION;
        }
        // 统一转大写，兼容测试或业务里不同大小写写法。
        String normalizedPurpose = purpose.toUpperCase();
        // 输出修复类调用消耗较小，但需要独立计数，方便后续观测和调参。
        if (normalizedPurpose.contains("REPAIR")) {
            return CHAT_REPAIR;
        }
        // 评估调用独立限流，避免批量评估压住主创作链路。
        if (normalizedPurpose.contains("EVALUATION")) {
            return CHAT_EVALUATION;
        }
        // 修正调用独立限流，便于控制 Reflection（反思修正）的额外成本。
        if (normalizedPurpose.contains("REVISION")) {
            return CHAT_REVISION;
        }
        // 其他聊天模型调用先归入创作生成池。
        return CHAT_GENERATION;
    }
}
