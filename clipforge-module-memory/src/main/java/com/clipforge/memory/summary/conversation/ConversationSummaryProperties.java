package com.clipforge.memory.summary.conversation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ConversationSummaryProperties（会话滚动摘要配置）。
 *
 * <p>该配置用于控制多轮会话何时触发 LLM（大语言模型）摘要，以及摘要 Prompt（提示词）文件路径。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.memory.conversation-summary")
public class ConversationSummaryProperties {

    /** enabled（是否启用）表示是否在创作前尝试刷新会话滚动摘要。 */
    private boolean enabled = true;

    /** recentMessageCount（最近消息保留数量）表示最近多少条消息永远保留原文，不参与摘要。 */
    private int recentMessageCount = 6;

    /** minSummarizableMessageCount（最小可摘要消息数量）表示候选旧消息少于该数量时不调用模型。 */
    private int minSummarizableMessageCount = 4;

    /** maxUnsummarizedChars（未摘要字符阈值）表示候选旧消息达到该字符数时触发摘要。 */
    private int maxUnsummarizedChars = 2_000;

    /** maxConversationHistoryTokens（会话历史词元阈值）表示完整会话历史估算超过该值时触发摘要。 */
    private int maxConversationHistoryTokens = 3_000;

    /** maxSummaryTokens（摘要最大输出词元）表示 LLM（大语言模型）生成摘要时的输出上限。 */
    private int maxSummaryTokens = 1_000;

    /** temperature（温度）表示摘要模型调用温度，低温更适合稳定压缩事实。 */
    private double temperature = 0.1;

    /** systemPath（系统提示词路径）定义摘要模型的稳定角色和边界。 */
    private String systemPath = "classpath:prompts/会话摘要-系统提示词.md";

    /** userPath（用户提示词路径）定义待摘要消息和历史摘要的动态输入格式。 */
    private String userPath = "classpath:prompts/会话摘要-用户提示词.md";
}
