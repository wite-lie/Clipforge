package com.clipforge.agent.intent;

/**
 * ConversationIntentRecognitionMethod（会话意图识别方式）。
 *
 * <p>该枚举用于标记一次意图识别来自确定性规则、LLM（大语言模型）分类还是降级兜底，
 * 方便后续 Trace（执行链路追踪）和问题复盘。
 */
public enum ConversationIntentRecognitionMethod {

    /** RULE（规则）表示高置信度规则直接命中。 */
    RULE,

    /** LLM（大语言模型）表示规则无法稳定判断后调用模型分类。 */
    LLM,

    /** FALLBACK（兜底）表示模型不可用、输出非法或置信度不足后采用保守路线。 */
    FALLBACK
}
