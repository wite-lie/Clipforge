package com.clipforge.prompt.token;

/**
 * PromptSectionType（提示词片段类型）。
 *
 * <p>该枚举用于标识 Prompt（提示词）中不同来源的文本，便于估算 Token（词元）占用和定位成本来源。
 */
public enum PromptSectionType {

    /** SYSTEM_PROMPT（系统提示词）表示 system（系统角色）消息。 */
    SYSTEM_PROMPT,

    /** USER_PROMPT（用户提示词）表示最终完整 user（用户角色）消息。 */
    USER_PROMPT,

    /** USER_REQUEST（用户请求）表示用户目标、平台和风格偏好等原始输入。 */
    USER_REQUEST,

    /** RAG_CONTEXT（检索增强上下文）表示 RAG（检索增强生成）命中的知识片段。 */
    RAG_CONTEXT,

    /** MEMORY_CONTEXT（记忆上下文）表示用户偏好、项目记忆、历史记忆和会话摘要。 */
    MEMORY_CONTEXT,

    /** IMAGE_SUMMARY（图片摘要）表示已经落库并注入创作链路的图片理解摘要。 */
    IMAGE_SUMMARY,

    /** VIDEO_SUMMARY（视频摘要）表示已经落库并注入创作链路的视频理解摘要。 */
    VIDEO_SUMMARY,

    /** CONVERSATION_HISTORY（会话历史）表示未来多轮对话中直接注入的历史消息。 */
    CONVERSATION_HISTORY,

    /** OUTPUT_SCHEMA（输出结构约束）表示要求模型输出 JSON（对象表示格式）等结构的规则。 */
    OUTPUT_SCHEMA,

    /** OTHER（其他）表示暂未归类的提示词片段。 */
    OTHER;

    /**
     * value（字符串值）用于写入日志、Trace（执行链路追踪）或测试输出。
     *
     * @return 片段类型字符串
     */
    public String value() {
        // 当前直接使用枚举名称作为稳定字符串。
        return name();
    }
}
